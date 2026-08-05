"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.stripeWebhook = exports.stripeWebhookHandler = exports.chargeFinalBalanceIntent = exports.createDepositPaymentIntent = exports.generateMicroSite = void 0;
const https_1 = require("firebase-functions/v2/https");
const logger = __importStar(require("firebase-functions/logger"));
const admin = __importStar(require("firebase-admin"));
const stripe_1 = __importDefault(require("stripe"));
// Initialize Firebase Admin SDK
if (!admin.apps.length) {
    admin.initializeApp();
}
const db = admin.firestore();
// Lazy-initialize Stripe client using environment variable STRIPE_SECRET_KEY
const getStripeClient = () => {
    const secretKey = process.env.STRIPE_SECRET_KEY || "sk_test_mock_quotecall_ai_key";
    return new stripe_1.default(secretKey, {
        apiVersion: "2023-10-16",
    });
};
/**
 * Helper to call Gemini 1.5 Flash REST API
 */
async function callGeminiApi(prompt, systemInstruction) {
    const apiKey = process.env.GEMINI_API_KEY || "AIzaSy_MOCK_KEY";
    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}`;
    const payload = {
        contents: [
            {
                role: "user",
                parts: [{ text: `${systemInstruction}\n\nUSER PROMPT: ${prompt}` }]
            }
        ],
        generationConfig: {
            temperature: 0.2,
            responseMimeType: "application/json"
        }
    };
    try {
        const response = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            const errText = await response.text();
            logger.warn("Gemini API non-200 response, using fallback generation", { errText });
            return null;
        }
        const data = await response.json();
        const content = data.candidates?.[0]?.content?.parts?.[0]?.text;
        if (content) {
            return JSON.parse(content);
        }
    }
    catch (err) {
        logger.warn("Gemini API call exception, using smart fallback", { error: err?.message });
    }
    return null;
}
/**
 * 1. Cloud Function: generateMicroSite
 * -----------------------------------------------------------------------------
 * Triggered when user clicks "✨ Generate Micro-Site" in the app or Site Builder.
 * Calls Gemini 1.5 Flash to construct structured JSON profile for business & micro-site.
 * Saves result to Firestore under `microsites/{slug}` and `sites/{slug}`.
 */
exports.generateMicroSite = (0, https_1.onCall)(async (request) => {
    const { prompt, companyProfile, userId } = request.data || {};
    const userPrompt = prompt || "Create a professional landing page and AI dispatcher for my field service business.";
    logger.info("Generating Micro-Site via Cloud Function", { userPrompt, userId });
    const systemPrompt = `
You are an expert AI business builder for field service contractors (plumbing, towing, HVAC, electrical, etc.).
Analyze the prompt and generate a JSON object matching this exact schema:
{
  "slug": "kebab-case-unique-business-slug",
  "business_name": "Full Professional Business Name",
  "tagline": "Short, catchy tagline",
  "theme": {
    "primaryColor": "#00E5FF",
    "themeColorHex": "#00E5FF",
    "darkStyle": true
  },
  "default_deposit": 50.00,
  "base_fee": 150.00,
  "system_instruction": "You are an AI Dispatcher for [Business Name]. Politely greet callers, collect issue details & address, and trigger create_stripe_deposit_link function for a $50 deposit."
}
Only output valid JSON. Do not include markdown formatting outside the JSON.
`;
    let generatedData = await callGeminiApi(userPrompt, systemPrompt);
    // Fallback structure if Gemini API key is missing or offline
    if (!generatedData || !generatedData.slug) {
        const fallbackName = companyProfile?.name || "Apex Field Services";
        const slug = fallbackName.toLowerCase().trim().replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "");
        generatedData = {
            slug: slug || "apex-services",
            business_name: fallbackName,
            tagline: "24/7 Rapid Response Field Service & Instant AI Dispatch",
            theme: {
                primaryColor: "#00E5FF",
                themeColorHex: "#00E5FF",
                darkStyle: true
            },
            default_deposit: companyProfile?.defaultDeposit || 50.00,
            base_fee: 150.00,
            system_instruction: `You are an emergency AI Service Dispatcher for ${fallbackName}. Assist callers in booking immediate service and collecting a $50 security deposit.`
        };
    }
    const slug = generatedData.slug || "service-pro";
    // Document payload to write to Firestore
    const microSiteDocument = {
        slug: slug,
        business_name: generatedData.business_name,
        companyName: generatedData.business_name,
        tagline: generatedData.tagline,
        siteTitle: generatedData.business_name,
        siteSubtitle: generatedData.tagline,
        theme: generatedData.theme,
        themeColorHex: generatedData.theme?.themeColorHex || "#00E5FF",
        default_deposit: generatedData.default_deposit || 50.00,
        quickDepositFee: generatedData.default_deposit || 50.00,
        base_fee: generatedData.base_fee || 150.00,
        system_instruction: generatedData.system_instruction,
        voiceCallButtonText: "Instant AI Dispatch / Live Voice",
        voiceCallDescription: "Speak directly with our AI agent to describe your emergency and request immediate help.",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };
    try {
        // 1. Save in Firestore collection `microsites/{slug}`
        await db.collection("microsites").doc(slug).set(microSiteDocument, { merge: true });
        // 2. Save in Firestore collection `sites/{slug}` for frontend router compatibility
        await db.collection("sites").doc(slug).set(microSiteDocument, { merge: true });
        // 3. If userId is attached, update user's webConfig
        if (userId) {
            await db.collection("users").doc(userId).collection("webConfig").doc("main").set(microSiteDocument, { merge: true });
        }
        logger.info(`Micro-site successfully saved to Firestore: microsites/${slug}`);
        return {
            success: true,
            slug: slug,
            config: microSiteDocument
        };
    }
    catch (error) {
        logger.error("Error saving microsite to Firestore", error);
        throw new https_1.HttpsError("internal", error.message || "Failed to persist micro-site configuration.");
    }
});
/**
 * 2. Cloud Function: createDepositPaymentIntent
 * -----------------------------------------------------------------------------
 * Step 1: Upfront Deposit Capture (Pre-Job)
 * Creates a Stripe PaymentIntent for the $50 (or custom) security deposit.
 * Passes `setup_future_usage = 'off_session'` to securely tokenize the card for
 * post-job balance settlement.
 * Deducts a 4.0% Platform Fee via Stripe Connect `application_fee_amount`.
 */
exports.createDepositPaymentIntent = (0, https_1.onCall)(async (request) => {
    const { jobId, amount, stripeAccountId, customerPhone, serviceTitle } = request.data;
    if (!jobId || !amount || !stripeAccountId) {
        throw new https_1.HttpsError("invalid-argument", "Missing required arguments: jobId, amount, or stripeAccountId.");
    }
    try {
        const stripe = getStripeClient();
        const amountInCents = Math.round(amount * 100);
        // 4.0% Platform Application Fee for Deposit Phase
        const applicationFeeAmount = Math.round(amountInCents * 0.04);
        logger.info(`Creating Deposit PaymentIntent for Job #${jobId}`, {
            amount,
            applicationFeeAmount,
            stripeAccountId,
        });
        // Create PaymentIntent on Contractor's Connected Account
        const paymentIntent = await stripe.paymentIntents.create({
            amount: amountInCents,
            currency: "usd",
            statement_descriptor_suffix: "DEPOSIT",
            setup_future_usage: "off_session", // Tokenize card for post-job balance charge
            application_fee_amount: applicationFeeAmount,
            metadata: {
                jobId: jobId,
                type: "DEPOSIT",
                customerPhone: customerPhone || "Unknown",
                serviceTitle: serviceTitle || "QuoteCall Deposit",
                description: "QUOTECALL DEPOSIT"
            },
        }, {
            stripeAccount: stripeAccountId, // Connected Contractor Account ID
        });
        // Persist PaymentIntent state to Firestore
        await db.collection("jobs").doc(jobId).set({
            depositPaymentIntentId: paymentIntent.id,
            depositAmount: amount,
            platformDepositFee: applicationFeeAmount / 100,
            stripeAccountId: stripeAccountId,
            status: "PENDING_DEPOSIT",
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, { merge: true });
        return {
            success: true,
            clientSecret: paymentIntent.client_secret,
            paymentIntentId: paymentIntent.id,
            depositAmount: amount,
            platformFee: applicationFeeAmount / 100,
        };
    }
    catch (error) {
        logger.error("Error creating deposit PaymentIntent", error);
        throw new https_1.HttpsError("internal", error.message || "Failed to create deposit PaymentIntent.");
    }
});
/**
 * 3. Cloud Function: chargeFinalBalanceIntent
 * -----------------------------------------------------------------------------
 * Step 2: Close Job & Final Balance Charge (Post-Job)
 * Charges the stored `payment_method_id` from Step 1 off-session.
 * Applies a discounted 1.5% incentive Platform Fee for final balance settlement.
 */
exports.chargeFinalBalanceIntent = (0, https_1.onCall)(async (request) => {
    const { jobId, balanceAmount, stripeAccountId, savedPaymentMethodId, customerId } = request.data;
    if (!jobId || !balanceAmount || !stripeAccountId || !savedPaymentMethodId) {
        throw new https_1.HttpsError("invalid-argument", "Missing required parameters: jobId, balanceAmount, stripeAccountId, or savedPaymentMethodId.");
    }
    try {
        const stripe = getStripeClient();
        const amountInCents = Math.round(balanceAmount * 100);
        // 1.5% Discounted Incentive Platform Fee for Final Balance
        const applicationFeeAmount = Math.round(amountInCents * 0.015);
        logger.info(`Executing Off-Session Final Balance Charge for Job #${jobId}`, {
            balanceAmount,
            applicationFeeAmount,
            stripeAccountId,
            savedPaymentMethodId,
        });
        const paymentIntent = await stripe.paymentIntents.create({
            amount: amountInCents,
            currency: "usd",
            payment_method: savedPaymentMethodId,
            customer: customerId || undefined,
            off_session: true,
            confirm: true,
            application_fee_amount: applicationFeeAmount,
            metadata: {
                jobId: jobId,
                type: "FINAL_BALANCE",
            },
        }, {
            stripeAccount: stripeAccountId,
        });
        const isSuccess = paymentIntent.status === "succeeded";
        const receiptUrl = paymentIntent.latest_charge
            ? `https://pay.stripe.com/receipts/${paymentIntent.id}`
            : undefined;
        if (isSuccess) {
            // Update Job status in Firestore to COMPLETED_PAID_STRIPE
            await db.collection("jobs").doc(jobId).update({
                status: "COMPLETED_PAID_STRIPE",
                settlementMethod: "SAVED_CARD",
                finalPaymentIntentId: paymentIntent.id,
                receiptUrl: receiptUrl,
                settledAt: admin.firestore.FieldValue.serverTimestamp(),
            });
        }
        return {
            success: isSuccess,
            status: paymentIntent.status,
            paymentIntentId: paymentIntent.id,
            receiptUrl: receiptUrl,
            amountPaid: balanceAmount,
            platformFee: applicationFeeAmount / 100,
        };
    }
    catch (error) {
        logger.error("Error executing off-session balance charge", error);
        throw new https_1.HttpsError("internal", error.message || "Failed to process final balance charge.");
    }
});
/**
 * 4. HTTPS Webhook Handler: stripeWebhookHandler & stripeWebhook
 * -----------------------------------------------------------------------------
 * Listens for Stripe Webhook events (`checkout.session.completed`, `payment_intent.succeeded`, `account.updated`).
 * Automatically updates Firestore database and triggers push/SMS notification summary to service provider.
 */
exports.stripeWebhookHandler = (0, https_1.onRequest)({ cors: true }, async (req, res) => {
    const sig = req.headers["stripe-signature"];
    const webhookSecret = process.env.STRIPE_WEBHOOK_SECRET;
    let event;
    try {
        const stripe = getStripeClient();
        if (sig && webhookSecret) {
            event = stripe.webhooks.constructEvent(req.rawBody, sig, webhookSecret);
        }
        else {
            // Fallback for development/testing webhook calls
            event = req.body;
        }
    }
    catch (err) {
        logger.error("Stripe Webhook signature verification failed", err);
        res.status(400).send(`Webhook Error: ${err.message}`);
        return;
    }
    logger.info(`Received Stripe Webhook Event: ${event?.type}`);
    if (!event || !event.type) {
        res.status(200).json({ received: true, note: "Empty event payload" });
        return;
    }
    switch (event.type) {
        case "checkout.session.completed": {
            const session = event.data.object;
            const customerEmail = session.customer_details?.email || "Unknown Customer";
            const customerName = session.customer_details?.name || "Client";
            const customerPhone = session.customer_details?.phone || session.metadata?.customerPhone || "N/A";
            // Address extraction
            const addressObj = session.customer_details?.address || session.shipping_details?.address;
            const formattedAddress = addressObj
                ? `${addressObj.line1 || ""}, ${addressObj.city || ""}, ${addressObj.state || ""} ${addressObj.postal_code || ""}`
                : "Address Provided on Site";
            const depositAmount = (session.amount_total || 5000) / 100;
            const jobId = session.metadata?.jobId || `job_${Date.now()}`;
            const serviceTitle = session.metadata?.serviceTitle || "Emergency Field Dispatch";
            logger.info(`Checkout session completed for ${customerName} ($${depositAmount})`, {
                customerEmail,
                customerPhone,
                formattedAddress,
                jobId
            });
            // Update Firestore job
            await db.collection("jobs").doc(jobId).set({
                status: "DEPOSIT_PAID",
                customerName,
                customerPhone,
                customerEmail,
                customerAddress: formattedAddress,
                depositAmount,
                serviceTitle,
                depositPaidAt: admin.firestore.FieldValue.serverTimestamp(),
                updatedAt: admin.firestore.FieldValue.serverTimestamp()
            }, { merge: true });
            // Send Push / SMS Notification Record to Service Provider
            const notificationSummary = `🚨 NEW $${depositAmount} DEPOSIT PAID!\nCustomer: ${customerName}\nPhone: ${customerPhone}\nAddress: ${formattedAddress}\nService: ${serviceTitle}`;
            await db.collection("notifications").add({
                title: `New Deposit Paid ($${depositAmount})`,
                message: notificationSummary,
                customerName,
                customerPhone,
                customerAddress: formattedAddress,
                depositAmount,
                type: "JOB_DEPOSIT_CONFIRMED",
                read: false,
                timestamp: admin.firestore.FieldValue.serverTimestamp()
            });
            logger.info("Notification log created for service provider:", notificationSummary);
            break;
        }
        case "payment_intent.succeeded": {
            const paymentIntent = event.data.object;
            const dispatchId = paymentIntent.metadata?.dispatchId || paymentIntent.metadata?.jobId || paymentIntent.id;
            const paymentType = paymentIntent.metadata?.type || "DEPOSIT";
            const depositAmount = (paymentIntent.amount_received || paymentIntent.amount || 5000) / 100;
            const customerName = paymentIntent.metadata?.customerName || "Customer";
            logger.info(`Received payment_intent.succeeded for dispatch #${dispatchId}`, {
                depositAmount,
                paymentType,
                paymentIntentId: paymentIntent.id
            });
            if (dispatchId) {
                const paymentMethodId = typeof paymentIntent.payment_method === "string"
                    ? paymentIntent.payment_method
                    : paymentIntent.payment_method?.id;
                // 1. Update Firestore document dispatches/{dispatchId} with status: 'PAID' and depositAmount
                await db.collection("dispatches").doc(dispatchId).set({
                    status: "PAID",
                    depositAmount: depositAmount,
                    paymentIntentId: paymentIntent.id,
                    savedPaymentMethodId: paymentMethodId || null,
                    paidAt: admin.firestore.FieldValue.serverTimestamp(),
                    updatedAt: admin.firestore.FieldValue.serverTimestamp()
                }, { merge: true });
                // Also update jobs/{dispatchId} for backwards compatibility
                try {
                    await db.collection("jobs").doc(dispatchId).set({
                        status: "PAID",
                        depositAmount: depositAmount,
                        savedPaymentMethodId: paymentMethodId || null,
                        depositPaidAt: admin.firestore.FieldValue.serverTimestamp(),
                        updatedAt: admin.firestore.FieldValue.serverTimestamp()
                    }, { merge: true });
                }
                catch (jobErr) {
                    logger.warn(`Job doc update error for #${dispatchId}`, jobErr);
                }
                logger.info(`Dispatch #${dispatchId} status updated to PAID via Webhook.`);
                // 2. Send Push Notification to contractor's Android application via FCM
                const pushTitle = "💰 Customer Deposit Received!";
                const pushBody = `Deposit of $${depositAmount.toFixed(2)} was successfully paid by ${customerName} for dispatch #${dispatchId}.`;
                try {
                    await admin.messaging().send({
                        topic: "contractors",
                        notification: {
                            title: pushTitle,
                            body: pushBody
                        },
                        data: {
                            dispatchId: dispatchId,
                            depositAmount: depositAmount.toString(),
                            type: "PAYMENT_RECEIVED",
                            status: "PAID"
                        },
                        android: {
                            priority: "high",
                            notification: {
                                sound: "default",
                                channelId: "dispatch_channel"
                            }
                        }
                    });
                    logger.info(`FCM Push notification sent for dispatch #${dispatchId}`);
                }
                catch (fcmErr) {
                    logger.warn("FCM push notification topic send result", fcmErr?.message || fcmErr);
                }
                // 3. Log notification to Firestore collection 'notifications'
                await db.collection("notifications").add({
                    title: pushTitle,
                    message: pushBody,
                    dispatchId: dispatchId,
                    depositAmount: depositAmount,
                    type: "STRIPE_DEPOSIT_PAID",
                    read: false,
                    timestamp: admin.firestore.FieldValue.serverTimestamp()
                });
            }
            break;
        }
        case "account.updated": {
            const account = event.data.object;
            logger.info(`Stripe Connect Account Updated: ${account.id}`, {
                charges_enabled: account.charges_enabled,
                payouts_enabled: account.payouts_enabled,
            });
            const companiesRef = db.collection("companies");
            const snapshot = await companiesRef.where("stripeAccountId", "==", account.id).get();
            snapshot.forEach(async (doc) => {
                await doc.ref.update({
                    stripeChargesEnabled: account.charges_enabled,
                    stripePayoutsEnabled: account.payouts_enabled,
                    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                });
            });
            break;
        }
        default:
            logger.info(`Unhandled event type ${event.type}`);
    }
    res.status(200).json({ received: true });
});
// Alias export for endpoint route /stripe-webhook
exports.stripeWebhook = exports.stripeWebhookHandler;
//# sourceMappingURL=index.js.map
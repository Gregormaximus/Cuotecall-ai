# QuoteCall AI - Firebase Cloud Functions (v2) Backend

This directory contains the production-ready Node.js / TypeScript Firebase Cloud Functions (v2) backend for **QuoteCall AI** (`com.quotecall.agent`).

---

## 🚀 Implemented Functions

### 1. `createDepositPaymentIntent` (onCall)
- **Purpose**: Creates an upfront $50 (or custom) deposit PaymentIntent on the contractor's Stripe Connect account (`stripeAccountId`).
- **Feature**: Passes `setup_future_usage: 'off_session'` to securely tokenize the customer's payment method for post-job settlement.
- **Platform Fee**: Deducts a **4.0%** application fee (`application_fee_amount`) for QuoteCall AI platform revenue.

### 2. `chargeFinalBalanceIntent` (onCall)
- **Purpose**: Charges the tokenized `savedPaymentMethodId` off-session when the contractor completes the job.
- **Platform Fee**: Applies a discounted **1.5%** incentive rate for post-job balance settlement.
- **Status Update**: Updates Firestore job status to `COMPLETED_PAID_STRIPE` and returns digital receipt link.

### 3. `stripeWebhookHandler` (onRequest HTTPS Endpoint)
- **Purpose**: Listens to Stripe events (`payment_intent.succeeded`, `account.updated`).
- **Actions**:
  - Sets job status to `DEPOSIT_PAID` upon deposit success.
  - Triggers Google Calendar 2-Way event sync and SMS confirmation dispatch.
  - Updates contractor onboarding status when Stripe Connect account capability changes.

---

## ⚙️ Environment Configuration

Set the required secret environment variables using Firebase Secret Manager:

```bash
firebase functions:secrets:set STRIPE_SECRET_KEY
firebase functions:secrets:set STRIPE_WEBHOOK_SECRET
```

---

## 🛠️ Deployment Instructions

1. **Install Dependencies**:
   ```bash
   cd functions
   npm install
   ```

2. **Build TypeScript**:
   ```bash
   npm run build
   ```

3. **Deploy Functions**:
   ```bash
   firebase deploy --only functions
   ```

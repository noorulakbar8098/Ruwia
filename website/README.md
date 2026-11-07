# Ruwia — Android App Distribution & Download Website

A modern, high-performance, responsive website designed for distributing Android application APKs publicly without relying on the Google Play Store.

Built with **Next.js (App Router, Static Export)**, **TypeScript**, **Tailwind CSS**, and **Lucide Icons**, inspired by the clean aesthetics of Linear, Stripe, Vercel, and Raycast.

---

## 🌟 Key Highlights

- **Linear / Vercel Aesthetic:** Emerald accent palette, subtle dark grids, glassmorphism headers, rounded cards, and smooth hover feedback.
- **Single-File Configuration:** Update app version, APK download links, checksums, features, screenshots, and FAQs in `src/config/app.config.ts`.
- **Zero Backend Required:** 100% static export (`output: 'export'`), ready to deploy for free on **GitHub Pages**, **Vercel**, **Cloudflare Pages**, or **Netlify**.
- **SHA-256 Checksum Verification:** One-click copy with terminal verification instructions (`sha256sum`).
- **Device Permission & Play Protect Guide:** Step-by-step installation instructions for Android 14–16, 8–13, and 7, explaining why "Allow from this source" prompts appear.
- **Interactive Screenshot Gallery:** Smartphone-framed previews with category filtering and keyboard-driven lightbox (Esc, Left/Right arrows).
- **Dark & Light Mode:** Persisted in `localStorage` with zero flicker on page load.
- **SEO & Search Indexing:** Automated `sitemap.xml`, `robots.txt`, OpenGraph cards, Twitter cards, and Schema.org `SoftwareApplication` JSON-LD.
- **Legal Compliance:** Dedicated Privacy Policy (`/privacy`) and Terms of Service (`/terms`) pages explaining Android permissions (Location, Notifications, Internet).

---

## 📂 Project Architecture

```
website/
├── public/
│   ├── images/
│   │   ├── app-icon.png           # 1254x1254 App icon
│   │   ├── logo.png               # Brand logo
│   │   ├── og-card.png            # OpenGraph card preview (1200x630)
│   │   └── screenshots/
│   │       ├── dashboard.svg      # Admin Dashboard screen mockup
│   │       ├── stock.svg          # Stock Ledger screen mockup
│   │       ├── deliveries.svg     # Delivery Dispatch screen mockup
│   │       └── billing.svg        # Customer Billing screen mockup
├── src/
│   ├── app/
│   │   ├── layout.tsx             # Root layout, fonts, SEO & theme detector
│   │   ├── page.tsx               # Homepage assembling all sections
│   │   ├── globals.css            # Tailwind theme, emerald gradients & styles
│   │   ├── privacy/page.tsx       # Full Privacy Policy page
│   │   ├── terms/page.tsx         # Full Terms of Service page
│   │   ├── sitemap.ts             # Dynamic SEO sitemap
│   │   └── robots.ts              # Robots.txt configuration
│   ├── components/
│   │   ├── Navbar.tsx             # Sticky responsive navigation & mobile menu
│   │   ├── Hero.tsx               # Headline, specs ribbon & interactive mockup
│   │   ├── Features.tsx           # Feature grid with Lucide icons
│   │   ├── ScreenshotsGallery.tsx # Filterable screenshots & lightbox modal
│   │   ├── DownloadCard.tsx       # Primary APK download card & SHA-256 copy
│   │   ├── InstallationGuide.tsx  # 6-step walkthrough & version tabs
│   │   ├── ReleaseHistory.tsx     # Changelog timeline & previous APK links
│   │   ├── FaqSection.tsx         # Searchable interactive accordion
│   │   ├── Footer.tsx             # Brand, links, social & legal disclaimers
│   │   └── ThemeToggle.tsx        # Dark / Light theme switcher
│   └── config/
│       └── app.config.ts          # ⭐️ CENTRALIZED CONFIGURATION (Edit this!)
├── next.config.ts                 # Static export configuration
├── package.json
└── tsconfig.json
```

---

## ⚙️ Configuration & Content Management

All website content is centralized in **[`src/config/app.config.ts`](./src/config/app.config.ts)**. You **never** need to touch UI components to release a new version or change copy.

### How to Release a New APK Version

Open `src/config/app.config.ts` and update the `release` block:

```ts
release: {
  version: "2.4",                          // Your new version name
  versionCode: 6,                          // Android versionCode
  releaseDate: "December 2026",             // Human-readable date
  apkSize: "28.9 MB",                      // File size
  minAndroid: "Android 7.0 (API 24)",      // Minimum OS supported
  targetAndroid: "Android 16 (API 36)",    // Target SDK
  architecture: "Universal",               // Architecture compatibility
  sha256: "YOUR_ACTUAL_SHA256_HASH_HERE",  // Generate using sha256sum
  downloadUrl: "https://github.com/your-org/your-repo/releases/download/v2.4/app-release.apk",
  githubReleaseUrl: "https://github.com/your-org/your-repo/releases/tag/v2.4",
  allReleasesUrl: "https://github.com/your-org/your-repo/releases",
}
```

### How to Calculate the Real SHA-256 Checksum

Run this command in your terminal on your built APK:

**macOS / Linux:**
```bash
shasum -a 256 app-release.apk
# OR
sha256sum app-release.apk
```

**Windows (PowerShell):**
```powershell
certutil -hashfile app-release.apk SHA256
```

Copy the 64-character hexadecimal hash into `app.config.ts`.

---

## 🚀 Local Development & Build

From within the `website/` directory:

```bash
# 1. Install dependencies
npm install

# 2. Start local development server (http://localhost:3000)
npm run dev

# 3. Build production static export (generates website/out/)
npm run build
```

---

## 🌐 Free Hosting & Deployment Guide

### Option A: Vercel (Recommended — 2 Minutes, Zero Setup)

1. Push your repository to GitHub.
2. Go to [Vercel](https://vercel.com) and log in.
3. Click **Add New Project** and select your `ruwia` repository.
4. Set the **Root Directory** to `website`.
5. Framework Preset will automatically detect **Next.js**.
6. Click **Deploy**. Vercel will build and assign you a free `https://your-app.vercel.app` domain with automatic SSL and global CDN.

### Option B: GitHub Pages (Live at https://uniwattelektrik.com)

A GitHub Actions workflow is pre-configured in [`.github/workflows/deploy-site.yml`](../.github/workflows/deploy-site.yml) and publishes `website/out` to GitHub Pages.

1. In your GitHub repository, navigate to **Settings** > **Pages**.
2. Under **Build and deployment** > **Source**, select **GitHub Actions**.
3. Push to `main`. The workflow builds `website/out` and deploys it with the custom domain declared in [`website/public/CNAME`](./public/CNAME).

#### DNS records (Squarespace Domains)

| Type | Host | Value |
| --- | --- | --- |
| A | `@` | `185.199.108.153` |
| A | `@` | `185.199.109.153` |
| A | `@` | `185.199.110.153` |
| A | `@` | `185.199.111.153` |
| CNAME | `www` | `noorulakbar8098.github.io` |

Preserve the existing Google Workspace MX records and verification TXT records. GitHub provisions the HTTPS certificate automatically after DNS resolves.

---

## 🛡️ Security Best Practices

- **Zero Secret Leakage:** No Supabase service-role keys, private API credentials, or passwords are used in the website code.
- **Signed APKs:** Only distribute APKs signed with your release keystore.
- **Integrity Validation:** Always publish the matching SHA-256 checksum so users and enterprise admins can verify the file has not been altered in transit.

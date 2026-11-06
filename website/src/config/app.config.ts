/**
 * ==============================================================================
 * CENTRALIZED APPLICATION CONFIGURATION
 * ==============================================================================
 * 
 * Edit this single file to update all information across the entire website:
 * - App version, release date, and APK download URLs
 * - SHA-256 checksums and technical specifications
 * - Features, screenshots, changelogs, and FAQs
 * - Support contacts, legal policies, and SEO metadata
 * 
 * All UI components automatically read from this configuration.
 */

export interface ReleaseItem {
  version: string;
  versionCode: number;
  releaseDate: string;
  isLatest: boolean;
  apkSize: string;
  sha256: string;
  downloadUrl: string;
  githubReleaseUrl: string;
  highlights: string;
  features: string[];
  improvements: string[];
  fixes: string[];
}

export interface FeatureItem {
  id: string;
  title: string;
  description: string;
  iconName: string; // Lucide icon name
  badge?: string;
  category: "operations" | "logistics" | "security" | "analytics";
}

export interface ScreenshotItem {
  id: string;
  title: string;
  description: string;
  category: "Dashboard" | "Inventory" | "Deliveries" | "Billing";
  imageUrl: string;
  alt: string;
}

export interface FaqItem {
  question: string;
  answer: string;
  category: "installation" | "security" | "general" | "updates";
}

// Empty for root deployments; "/Ruwia" when built for a GitHub Pages project
// site. Next.js inlines NEXT_PUBLIC_* values at build time.
const BASE_PATH = process.env.NEXT_PUBLIC_BASE_PATH ?? "";

const publicAsset = (path: string) => `${BASE_PATH}${path}`;

export const APP_CONFIG = {
  // ----------------------------------------------------------------------------
  // Basic Application Identity
  // ----------------------------------------------------------------------------
  app: {
    name: "RUWIA",
    tagline: "Water & Beverage Distribution Management",
    shortDescription:
      "The complete operational platform for water can distributors, delivery drivers, and multi-shop inventory managers.",
    heroHeadline: "Technology that works around you.",
    heroDescription:
      "A thoughtfully built digital experience, designed to make everyday tasks simpler, faster, and more connected. Download the verified production Android application directly.",
    packageName: "com.example.ruwia",
    logoPath: publicAsset("/images/logo.png"),
    appIconPath: publicAsset("/images/app-icon.png"),
    heroScreenshotPath: publicAsset("/images/screenshots/Screenshot_20261107_001233.png"),
    featureScreenshotPath: publicAsset("/images/screenshots/Screenshot_20261107_001300.png"),
  },

  // ----------------------------------------------------------------------------
  // Current Production Release Details
  // EDIT THESE VALUES WHEN PUBLISHING A NEW APK VERSION
  // ----------------------------------------------------------------------------
  release: {
    version: "2.4",
    versionCode: 6,
    releaseDate: "November 7, 2026",
    apkSize: "26.8 MB",
    minAndroid: "Android 7.0 (Nougat, API 24)",
    targetAndroid: "Android 16 (API 36)",
    architecture: "Universal (arm64-v8a, armeabi-v7a, x86_64)",
    // Genuine SHA-256 checksum of the published APK.
    // Verify with: shasum -a 256 ruwia-v2.4-release.apk
    sha256: "9f32a7fcc1f6ab79357f803892abb139bba5f19bb1b52ceb223872156b2577dd",

    // The signed APK is served as a static file from this website.
    // One click downloads it directly; no GitHub or external portal involved.
    downloadUrl: publicAsset("/downloads/ruwia-v2.4-release.apk"),

    // Source repository and release archive (secondary references only).
    githubReleaseUrl: "https://github.com/noorulakbar8098/Ruwia/releases",
    allReleasesUrl: "https://github.com/noorulakbar8098/Ruwia/releases",
  },

  // ----------------------------------------------------------------------------
  // Links & External Resources
  // ----------------------------------------------------------------------------
  links: {
    githubRepo: "https://github.com/noorulakbar8098/Ruwia",
    githubIssues: "https://github.com/noorulakbar8098/Ruwia/issues",
    supportEmail: "support@ruwia.app",
    telegramSupport: "https://t.me/ruwia_support",
    whatsappSupport: "https://wa.me/919876543210",
    docsUrl: "https://github.com/noorulakbar8098/Ruwia#readme",
  },

  // ----------------------------------------------------------------------------
  // Key Features
  // ----------------------------------------------------------------------------
  features: [
    {
      id: "stock-ledger",
      title: "Real-Time Stock Ledger",
      description:
        "Instantly track full water cans, empty return cases, and buffer stocks across multiple shops and central warehouses with zero lag.",
      iconName: "Boxes",
      badge: "Core Engine",
      category: "operations",
    },
    {
      id: "delivery-dispatch",
      title: "Delivery Task & GPS Dispatch",
      description:
        "Assign customer orders to delivery employees, track route progress in real time with location services, and confirm drop-offs on-site.",
      iconName: "Truck",
      badge: "Logistics",
      category: "logistics",
    },
    {
      id: "can-accounting",
      title: "Can Ledger & Deposit Accounting",
      description:
        "Never lose a 20L can again. Track can deposits, bottle balances per customer, and automatically balance outgoing vs. returned empties.",
      iconName: "RotateCcw",
      badge: "Accounting",
      category: "operations",
    },
    {
      id: "pos-billing",
      title: "Customer Sales & Billing",
      description:
        "Generate point-of-sale invoices, record cash or UPI payments, maintain customer credit balances, and share receipts instantly.",
      iconName: "Receipt",
      badge: "Fast Checkout",
      category: "operations",
    },
    {
      id: "push-alerts",
      title: "Instant Push Notifications",
      description:
        "Get instant Firebase Cloud Messaging (FCM) notifications for new delivery assignments, low inventory thresholds, and customer payments.",
      iconName: "BellRing",
      badge: "Real-time",
      category: "security",
    },
    {
      id: "offline-sync",
      title: "Offline-First Resilience",
      description:
        "Keep recording sales and stock movements even without cellular signal. All transactions automatically sync securely once reconnected.",
      iconName: "WifiOff",
      badge: "Enterprise",
      category: "security",
    },
  ] as FeatureItem[],

  // ----------------------------------------------------------------------------
  // Screenshots Gallery
  // ----------------------------------------------------------------------------
  screenshots: [
    {
      id: "screen-owner-dashboard",
      title: "Real-Time Owner Dashboard",
      description: "Comprehensive live KPIs: monthly net profit, total customers, completed routes, and revenue trends.",
      category: "Dashboard",
      imageUrl: publicAsset("/images/screenshots/Screenshot_20261107_001233.png"),
      alt: "RUWIA Owner Business Dashboard with live KPIs",
    },
    {
      id: "screen-driver-portal",
      title: "Driver & Shop Daily Portal",
      description: "Shift overview with 45 cases live inventory, 21 empty returns balance, and one-tap quick actions.",
      category: "Dashboard",
      imageUrl: publicAsset("/images/screenshots/Screenshot_20261107_001703.png"),
      alt: "RUWIA Driver & Shop Daily Portal",
    },
    {
      id: "screen-inventory-live",
      title: "Multi-Shop Stock Ledger",
      description: "Live overview across all shops with unit breakdown, per-product cost valuation, and health indicators.",
      category: "Inventory",
      imageUrl: publicAsset("/images/screenshots/Screenshot_20261107_001300.png"),
      alt: "RUWIA Multi-Shop Inventory Ledger",
    },
    {
      id: "screen-water-dispatch",
      title: "Water Dispatch Outflow Trends",
      description: "Visual case dispatch volume over time, delivered units tally, and empty returns reconciliation.",
      category: "Inventory",
      imageUrl: publicAsset("/images/screenshots/Screenshot_20261107_001723.png"),
      alt: "RUWIA Water Dispatch and Outflow Trends",
    },
    {
      id: "screen-delivery-gps",
      title: "Live GPS Route Tracking",
      description: "Real-time delivery journey activity with breadcrumbs and latitude and longitude coordinates.",
      category: "Deliveries",
      imageUrl: publicAsset("/images/screenshots/Screenshot_20261107_001428.png"),
      alt: "RUWIA Live Route Updates and GPS Breadcrumbs",
    },
    {
      id: "screen-map-dispatch",
      title: "Interactive Map Dispatch",
      description: "Pinpoint customer drop-off locations directly on an interactive map with auto-detected GPS coordinates.",
      category: "Deliveries",
      imageUrl: publicAsset("/images/screenshots/Screenshot_20261107_001437.png"),
      alt: "RUWIA Map Dispatch and Geolocation Pinning",
    },
    {
      id: "screen-delivery-queue",
      title: "Delivery Task Dispatch Queue",
      description: "Schedule vs completed route audits, fast task filtering, and instant new delivery creation.",
      category: "Deliveries",
      imageUrl: publicAsset("/images/screenshots/Screenshot_20261107_001414.png"),
      alt: "RUWIA Delivery Task Dispatch Queue",
    },
    {
      id: "screen-sales-transactions",
      title: "Sales & Transactions Ledger",
      description: "Comprehensive 33-entry sales ledger with individual profit margins, customer names, and receipt IDs.",
      category: "Billing",
      imageUrl: publicAsset("/images/screenshots/Screenshot_20261107_001308.png"),
      alt: "RUWIA Sales History and Transactions Ledger",
    },
    {
      id: "screen-payments-dues",
      title: "Payments & Outstanding Dues",
      description: "Track billed vs collected amounts, dues alerts per customer, and instant UPI or Cash payment logs.",
      category: "Billing",
      imageUrl: publicAsset("/images/screenshots/Screenshot_20261107_001401.png"),
      alt: "RUWIA Payments and Dues Collection",
    },
    {
      id: "screen-can-accounting",
      title: "Customer Bottle & Can Accounting",
      description: "Customer profile tracking empty bottles in their possession (50 cans with them) and purchase history.",
      category: "Billing",
      imageUrl: publicAsset("/images/screenshots/Screenshot_20261107_001610.png"),
      alt: "RUWIA Customer Bottle and Can Accounting",
    },
    {
      id: "screen-sales-profit",
      title: "Profitability & Margin Analytics",
      description: "Net profit analytics, revenue vs expense curves, and 71.58% net margin performance insights.",
      category: "Billing",
      imageUrl: publicAsset("/images/screenshots/Screenshot_20261107_001330.png"),
      alt: "RUWIA Sales & Profit Margin Analytics",
    },
    {
      id: "screen-auth-portal",
      title: "Enterprise Role Authentication",
      description: "Unified secure sign-in portal with dedicated Admin and Employee profile access toggles.",
      category: "Dashboard",
      imageUrl: publicAsset("/images/screenshots/Screenshot_20261107_001156.png"),
      alt: "RUWIA Enterprise Role Authentication",
    },
  ] as ScreenshotItem[],

  // ----------------------------------------------------------------------------
  // Release History & Changelogs
  // ----------------------------------------------------------------------------
  releases: [
    {
      version: "2.4",
      versionCode: 6,
      releaseDate: "November 7, 2026",
      isLatest: true,
      apkSize: "26.8 MB",
      sha256: "9f32a7fcc1f6ab79357f803892abb139bba5f19bb1b52ceb223872156b2577dd",
      downloadUrl: publicAsset("/downloads/ruwia-v2.4-release.apk"),
      githubReleaseUrl: "https://github.com/noorulakbar8098/Ruwia/releases",
      highlights: "Redesigned Payments & Dues with a collection dashboard, customer ledger, and one-tap payment recording.",
      features: [
        "Outstanding balance hero with collection progress, billed vs collected totals, and due-customer count.",
        "Segmented Outstanding and All Customers views with search, recent payment feed, and customer cards.",
        "Per-customer ledger statement with sale and payment entries, running balance, and statement totals.",
        "Record full or partial payments with Cash, UPI, Card, and Bank modes plus optional notes.",
        "Glossy deep-teal surfaces with mint highlights applied consistently to primary actions and payment surfaces.",
        "Background delivery location tracking and FCM push notifications for new tasks and stock alerts.",
      ],
      improvements: [
        "Faster dashboard loading through lazy section rendering.",
        "Cumulative customer growth graph and refined business insight cards.",
        "Consistent mint iconography and deep-teal action styling across the app.",
        "Optimized Supabase queries for snappy low-bandwidth operation.",
      ],
      fixes: [
        "Validated payment amounts, full-balance shortcuts, and clearer payment errors.",
        "Stability fixes across employee dashboard and transaction screens.",
      ],
    },
    {
      version: "2.3",
      versionCode: 5,
      releaseDate: "November 2026",
      isLatest: false,
      apkSize: "28.4 MB",
      sha256: "",
      downloadUrl: "",
      githubReleaseUrl: "https://github.com/noorulakbar8098/Ruwia/releases",
      highlights: "Added background delivery location tracking, push notifications, and updated to Android 16 target SDK.",
      features: [
        "FCM background push notifications for new delivery task dispatches and stock alerts.",
        "Background location tracking support with proper foreground service notification channels.",
        "Updated target SDK to Android 16 (API 36) with optimized tablet/foldable landscape layout handling.",
      ],
      improvements: [
        "Optimized Supabase postgrest query caching for snappy low-bandwidth operation.",
        "Refined customer sales UI with faster one-tap quantity increments.",
        "Reduced overall APK size by 18% through resource optimization.",
      ],
      fixes: [
        "Fixed notification icon resolution on modern Android 14+ devices.",
        "Resolved intermittent token refresh delay when resuming app from deep sleep.",
      ],
    },
    {
      version: "2.2",
      versionCode: 4,
      releaseDate: "October 2026",
      isLatest: false,
      apkSize: "29.1 MB",
      sha256: "",
      downloadUrl: "",
      githubReleaseUrl: "https://github.com/noorulakbar8098/Ruwia/releases",
      highlights: "Customer payment ledger migration and enhanced delivery task management.",
      features: [
        "Customer payment history ledger with cash, UPI, and credit settlement records.",
        "Automated delivery dispatch assignment based on driver proximity.",
      ],
      improvements: [
        "Enhanced stock movement export to CSV for accounting reconciliations.",
        "Added visual vibration feedback when confirming completed deliveries.",
      ],
      fixes: [
        "Fixed ledger calculation glitch when partial payments were recorded during offline mode.",
      ],
    },
    {
      version: "2.0",
      versionCode: 2,
      releaseDate: "September 2026",
      isLatest: false,
      apkSize: "27.8 MB",
      sha256: "",
      downloadUrl: "",
      githubReleaseUrl: "https://github.com/noorulakbar8098/Ruwia/releases",
      highlights: "Empty cans baseline persistence, multi-shop tenant isolation, and unlimited movement audit history.",
      features: [
        "Multi-shop stock ledger with isolated shop inventory chips and reset baselines.",
        "Unlimited historical movement feed without truncation.",
      ],
      improvements: [
        "Service-role fallback retry for administrative database operations.",
        "Consistent empty-can filter logic across all employee and admin tabs.",
      ],
      fixes: [
        "Fixed empty cases reset persistence on both admin dashboard and employee stock chips.",
        "Fixed stock movement truncation under high transaction volume.",
      ],
    },
  ] as ReleaseItem[],

  // ----------------------------------------------------------------------------
  // Frequently Asked Questions
  // ----------------------------------------------------------------------------
  faqs: [
    {
      question: "Is this the official application?",
      answer:
        "Yes, this is the official, authentic build of Ruwia built directly from our production repository. Each APK is cryptographically signed with our verified release signing key. You can also verify the authenticity of the file by comparing the SHA-256 checksum published above with your downloaded APK.",
      category: "general",
    },
    {
      question: "Is downloading free?",
      answer:
        "Yes, downloading and installing the Ruwia Android APK directly from our official website is 100% free of charge. There are no download fees or hidden charges.",
      category: "general",
    },
    {
      question: "Why is Google Play not required?",
      answer:
        "By distributing our official APK directly through this website, we provide our users with instantaneous updates, zero regional restrictions, and immediate hotfixes without waiting days for third-party app store approval queues. You retain total ownership of your installation.",
      category: "general",
    },
    {
      question: "How do I install the APK?",
      answer:
        "Installation takes less than 30 seconds: (1) Tap the 'Download APK' button above. The file downloads directly from this website. (2) When the download completes, open your browser's download notification or the Downloads app. (3) Tap 'ruwia-v2.4-release.apk'. (4) If prompted, toggle 'Allow from this source'. (5) Tap 'Install' and launch the app. Android does not allow a website to open its package installer automatically, so the final tap always happens on your device.",
      category: "installation",
    },
    {
      question: "How do I update the application?",
      answer:
        "When an update is released, simply download the new APK from this website and tap install. Android will recognize the same signature and package name and will update your app in-place without deleting your data, settings, or offline history.",
      category: "updates",
    },
    {
      question: "Is my information secure?",
      answer:
        "Yes. Ruwia connects directly to encrypted Supabase endpoints over TLS 1.3 encryption. We never access your contacts, private photos, or personal messages. Only necessary permissions (Internet, Notifications, and Location for delivery tracking) are requested.",
      category: "security",
    },
    {
      question: "What should I do if installation fails?",
      answer:
        "If you encounter 'App not installed' or 'Installation blocked': (1) Ensure you have at least 100 MB of free storage space. (2) Check that your device is running Android 7.0 or newer. (3) If Google Play Protect shows a dialog, tap 'More details' and then tap 'Install anyway'. (4) If you previously installed the v2.3 build, uninstall it once before installing v2.4. This release uses a new signing certificate, and Android will not update an app across different signatures. Server data stays safe in your account and syncs back after you sign in.",
      category: "installation",
    },
  ] as FaqItem[],

  // ----------------------------------------------------------------------------
  // Installation Steps
  // ----------------------------------------------------------------------------
  installationSteps: [
    {
      step: 1,
      title: "Tap Download APK",
      description: "Tap the primary download button to retrieve the latest signed APK directly from this website.",
      tip: "The download starts immediately. There is no external portal or GitHub detour.",
    },
    {
      step: 2,
      title: "Wait for Download",
      description: "Track download progress in your browser notification area. The APK is 26.8 MB.",
      tip: "A standard 4G or Wi-Fi connection takes only 2–5 seconds.",
    },
    {
      step: 3,
      title: "Open Downloaded File",
      description: "Tap the download completed notification, or locate 'ruwia-v2.4-release.apk' in your Downloads folder.",
      tip: "You can also access it via Chrome's Downloads tab (Ctrl+J or Menu > Downloads).",
    },
    {
      step: 4,
      title: "Grant Install Permission",
      description: "If Android displays 'For your security, your phone is not allowed to install unknown apps', tap Settings and switch on 'Allow from this source'.",
      tip: "This is a standard Android security measure for any app installed outside Google Play.",
    },
    {
      step: 5,
      title: "Tap Install",
      description: "Confirm the installation dialog and allow the package installer to finalize the setup.",
      tip: "If Play Protect prompts you, tap 'More details' → 'Install anyway'.",
    },
    {
      step: 6,
      title: "Launch & Get Started",
      description: "Tap Open to launch Ruwia. Log in with your admin or employee credentials and start distributing!",
      tip: "The app icon will now appear on your home screen and app drawer.",
    },
  ],

  // ----------------------------------------------------------------------------
  // Android Compatibility Guide Notes
  // ----------------------------------------------------------------------------
  androidVersionNotes: {
    android14Plus: "Android 14, 15, & 16: Go to Settings > Apps > Special app access > Install unknown apps > Select your browser (e.g., Chrome) > Toggle 'Allow from this source'.",
    android8to13: "Android 8.0 to 13: Tap the downloaded file. When prompted, tap 'Settings', turn on 'Allow from this source', then tap Back and tap 'Install'.",
    android7: "Android 7.0 & 7.1: Go to Settings > Security > Check 'Unknown sources' > Tap OK.",
  },

  // ----------------------------------------------------------------------------
  // SEO & OpenGraph Configuration
  // ----------------------------------------------------------------------------
  seo: {
    title: "Ruwia - Official Android App Direct Download | Latest APK",
    description: "Download the official signed Ruwia Android APK directly. Real-time stock ledger, water can distribution, delivery dispatch, and billing for modern operations.",
    keywords: [
      "Ruwia APK download",
      "water can delivery app",
      "stock ledger Android",
      "distribution management APK",
      "direct APK download",
      "Android inventory app",
      "Ruwia Android release",
    ],
    siteUrl: process.env.NEXT_PUBLIC_SITE_URL || "https://ruwia.app",
    ogImageUrl: publicAsset("/images/og-card.png"),
    twitterHandle: "@ruwia_app",
  },
};

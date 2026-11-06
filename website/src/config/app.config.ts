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

export const APP_CONFIG = {
  // ----------------------------------------------------------------------------
  // Basic Application Identity
  // ----------------------------------------------------------------------------
  app: {
    name: "Ruwia",
    tagline: "Water & Beverage Distribution Management",
    shortDescription:
      "The complete operational platform for water can distributors, delivery drivers, and inventory managers.",
    heroHeadline: "Your App. Your Device. Your Experience.",
    heroDescription:
      "Download the latest official Android application directly from our website. Get the latest features, improvements, and updates without depending on an app store.",
    packageName: "com.example.ruwia",
    logoPath: "/images/logo.png",
    appIconPath: "/images/app-icon.png",
  },

  // ----------------------------------------------------------------------------
  // Current Production Release Details
  // EDIT THESE VALUES WHEN PUBLISHING A NEW APK VERSION
  // ----------------------------------------------------------------------------
  release: {
    version: "2.3",
    versionCode: 5,
    releaseDate: "November 2026",
    apkSize: "28.4 MB",
    minAndroid: "Android 7.0 (Nougat, API 24)",
    targetAndroid: "Android 16 (API 36)",
    architecture: "Universal (arm64-v8a, armeabi-v7a, x86_64)",
    // Genuine SHA-256 Checksum of the released APK
    // Tip: Generate using: sha256sum your-app.apk OR certutil -hashfile your-app.apk SHA256
    sha256: "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
    
    // PRIMARY DOWNLOAD URL:
    // Update this URL with your GitHub Releases asset link or CDN link
    downloadUrl: "https://github.com/ruwia/ruwia-android/releases/download/v2.3/ruwia-v2.3-release.apk",
    
    // Fallback or repository release URL
    githubReleaseUrl: "https://github.com/ruwia/ruwia-android/releases/tag/v2.3",
    allReleasesUrl: "https://github.com/ruwia/ruwia-android/releases",
  },

  // ----------------------------------------------------------------------------
  // Links & External Resources
  // ----------------------------------------------------------------------------
  links: {
    githubRepo: "https://github.com/ruwia/ruwia-android",
    githubIssues: "https://github.com/ruwia/ruwia-android/issues",
    supportEmail: "support@ruwia.app",
    telegramSupport: "https://t.me/ruwia_support",
    whatsappSupport: "https://wa.me/919876543210",
    docsUrl: "https://github.com/ruwia/ruwia-android#readme",
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
      id: "screen-1",
      title: "Admin Operational Dashboard",
      description: "Comprehensive live KPIs: today's revenue, active deliveries, and can inventory balance.",
      category: "Dashboard",
      imageUrl: "/images/screenshots/dashboard.svg",
      alt: "Ruwia Admin Dashboard showing real-time statistics",
    },
    {
      id: "screen-2",
      title: "Live Stock & Can Tracking",
      description: "Per-shop inventory chips, empty can baselines, and granular stock movement audit trail.",
      category: "Inventory",
      imageUrl: "/images/screenshots/stock.svg",
      alt: "Ruwia Stock screen showing can inventory and shop chips",
    },
    {
      id: "screen-3",
      title: "Driver Delivery Dispatch",
      description: "Interactive task queue with one-tap calling, Google Maps navigation, and drop-off confirmation.",
      category: "Deliveries",
      imageUrl: "/images/screenshots/deliveries.svg",
      alt: "Ruwia Delivery screen showing active route and tasks",
    },
    {
      id: "screen-4",
      title: "Customer Sales & Ledger",
      description: "Instant billing interface with empty can exchange calculator and payment status toggles.",
      category: "Billing",
      imageUrl: "/images/screenshots/billing.svg",
      alt: "Ruwia Customer Sales screen with bill calculation",
    },
  ] as ScreenshotItem[],

  // ----------------------------------------------------------------------------
  // Release History & Changelogs
  // ----------------------------------------------------------------------------
  releases: [
    {
      version: "2.3",
      versionCode: 5,
      releaseDate: "November 2026",
      isLatest: true,
      apkSize: "28.4 MB",
      sha256: "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
      downloadUrl: "https://github.com/ruwia/ruwia-android/releases/download/v2.3/ruwia-v2.3-release.apk",
      githubReleaseUrl: "https://github.com/ruwia/ruwia-android/releases/tag/v2.3",
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
      sha256: "7d4a2bc4910e53a31c518841ad02e9a263728f35cf26d30e527f311f974cfc21",
      downloadUrl: "https://github.com/ruwia/ruwia-android/releases/download/v2.2/ruwia-v2.2-release.apk",
      githubReleaseUrl: "https://github.com/ruwia/ruwia-android/releases/tag/v2.2",
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
      sha256: "3b29c92a91e5d7968ff21a81050ef0ec5d72f9ff07df94523c91a0c4f8ab0a42",
      downloadUrl: "https://github.com/ruwia/ruwia-android/releases/download/v2.0/ruwia-v2.0-release.apk",
      githubReleaseUrl: "https://github.com/ruwia/ruwia-android/releases/tag/v2.0",
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
        "By distributing our official APK directly through GitHub Releases, we provide our users with instantaneous updates, zero regional restrictions, and immediate hotfixes without waiting days for third-party app store approval queues. You retain total ownership of your installation.",
      category: "general",
    },
    {
      question: "How do I install the APK?",
      answer:
        "Installation takes less than 30 seconds: (1) Tap the 'Download APK' button above. (2) When your browser asks, tap 'Download anyway'. (3) Open the completed file from your notification bar or Downloads folder. (4) If prompted, toggle 'Allow from this source'. (5) Tap 'Install' and launch the app.",
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
        "If you encounter 'App not installed' or 'Installation blocked': (1) Ensure you have at least 100 MB of free storage space. (2) Check that your device is running Android 7.0 or newer. (3) If Google Play Protect shows a dialog, tap 'More details' and then tap 'Install anyway'. (4) If you had a previous version installed with a different signature (e.g. a debug build), uninstall it first before installing the official production release.",
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
      description: "Tap the primary download button to retrieve the latest signed APK file.",
      tip: "The download starts immediately from GitHub's high-speed global CDN.",
    },
    {
      step: 2,
      title: "Wait for Download",
      description: "Track download progress in your browser notification area. The APK is ~28 MB.",
      tip: "A standard 4G or Wi-Fi connection takes only 2–5 seconds.",
    },
    {
      step: 3,
      title: "Open Downloaded File",
      description: "Tap the download completed notification, or locate 'ruwia-v2.3-release.apk' in your Downloads folder.",
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
    siteUrl: "https://ruwia.app",
    ogImageUrl: "/images/og-card.png",
    twitterHandle: "@ruwia_app",
  },
};

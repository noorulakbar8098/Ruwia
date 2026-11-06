import type { Metadata, Viewport } from "next";
import "./globals.css";
import { APP_CONFIG } from "@/config/app.config";
import { DownloadProvider } from "@/context/DownloadContext";
import InstallDialogWrapper from "@/components/InstallDialogWrapper";

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: dark)", color: "#06090e" },
    { media: "(prefers-color-scheme: light)", color: "#fbfcfd" },
  ],
  width: "device-width",
  initialScale: 1,
};

export const metadata: Metadata = {
  metadataBase: new URL(APP_CONFIG.seo.siteUrl),
  title: {
    default: APP_CONFIG.seo.title,
    template: `%s | ${APP_CONFIG.app.name}`,
  },
  description: APP_CONFIG.seo.description,
  keywords: APP_CONFIG.seo.keywords,
  authors: [{ name: `${APP_CONFIG.app.name} Team` }],
  creator: APP_CONFIG.app.name,
  publisher: APP_CONFIG.app.name,
  icons: {
    icon: [
      { url: APP_CONFIG.app.logoPath, sizes: "32x32", type: "image/png" },
      { url: APP_CONFIG.app.appIconPath, sizes: "192x192", type: "image/png" },
    ],
    apple: [
      { url: APP_CONFIG.app.appIconPath, sizes: "180x180", type: "image/png" },
    ],
  },
  openGraph: {
    type: "website",
    locale: "en_US",
    url: APP_CONFIG.seo.siteUrl,
    title: APP_CONFIG.seo.title,
    description: APP_CONFIG.seo.description,
    siteName: APP_CONFIG.app.name,
    images: [
      {
        url: APP_CONFIG.seo.ogImageUrl,
        width: 1200,
        height: 630,
        alt: `${APP_CONFIG.app.name} Android APK Download`,
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: APP_CONFIG.seo.title,
    description: APP_CONFIG.seo.description,
    images: [APP_CONFIG.seo.ogImageUrl],
    creator: APP_CONFIG.seo.twitterHandle,
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      "max-video-preview": -1,
      "max-image-preview": "large",
      "max-snippet": -1,
    },
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <script
          dangerouslySetInnerHTML={{
            __html: `
              (function() {
                try {
                  var stored = localStorage.getItem('ruwia-theme');
                  if (stored === 'dark') {
                    document.documentElement.classList.add('dark');
                  } else if (stored === 'light') {
                    document.documentElement.classList.remove('dark');
                  } else if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
                    document.documentElement.classList.add('dark');
                  }
                } catch (e) {}
              })();
            `,
          }}
        />
      </head>
      <body className="min-h-screen flex flex-col font-sans antialiased bg-[#F7F7F2] dark:bg-[#111714] text-[#202521] dark:text-[#F3F6F1] selection:bg-[#173D32] dark:selection:bg-[#C8D8B9] selection:text-[#F7F7F2] dark:selection:text-[#111714]">
        <DownloadProvider>
          {children}
          <InstallDialogWrapper />
        </DownloadProvider>
      </body>
    </html>
  );
}

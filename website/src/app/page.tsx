import Navbar from "@/components/Navbar";
import Hero from "@/components/Hero";
import ProductIntro from "@/components/ProductIntro";
import Features from "@/components/Features";
import ScreenshotsGallery from "@/components/ScreenshotsGallery";
import DownloadCard from "@/components/DownloadCard";
import InstallationGuide from "@/components/InstallationGuide";
import ReleaseHistory from "@/components/ReleaseHistory";
import FaqSection from "@/components/FaqSection";
import Footer from "@/components/Footer";
import { APP_CONFIG } from "@/config/app.config";

export default function HomePage() {
  // Schema.org structured data for Google Search SEO
  const jsonLd = {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    "name": APP_CONFIG.app.name,
    "operatingSystem": "Android",
    "applicationCategory": "BusinessApplication",
    "softwareVersion": APP_CONFIG.release.version,
    "fileSize": APP_CONFIG.release.apkSize,
    "datePublished": APP_CONFIG.release.releaseDate,
    "downloadUrl": APP_CONFIG.release.downloadUrl,
    "offers": {
      "@type": "Offer",
      "price": "0.00",
      "priceCurrency": "USD",
    },
    "description": APP_CONFIG.app.shortDescription,
  };

  return (
    <>
      {/* Structured data injection */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />

      <div className="flex flex-col min-h-screen">
        <Navbar />
        <main className="flex-1">
          <Hero />
          <ProductIntro />
          <Features />
          <ScreenshotsGallery />
          <DownloadCard />
          <InstallationGuide />
          <ReleaseHistory />
          <FaqSection />
        </main>
        <Footer />
      </div>
    </>
  );
}

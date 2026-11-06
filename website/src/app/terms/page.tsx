import type { Metadata } from "next";
import Link from "next/link";
import { ArrowLeft, FileText, CheckCircle2 } from "lucide-react";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { APP_CONFIG } from "@/config/app.config";

export const metadata: Metadata = {
  title: `Terms & Conditions - ${APP_CONFIG.app.name}`,
  description: `Official terms of service for downloading, installing, and using the ${APP_CONFIG.app.name} Android application.`,
};

export default function TermsPage() {
  return (
    <div className="flex flex-col min-h-screen">
      <Navbar />

      <main className="flex-1 py-14 sm:py-20">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          
          {/* Back Navigation */}
          <Link
            href="/"
            className="inline-flex items-center gap-2 text-sm font-semibold text-slate-600 dark:text-slate-400 hover:text-emerald-500 mb-8 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Back to Home</span>
          </Link>

          {/* Header */}
          <div className="border-b border-slate-200 dark:border-slate-800 pb-8 mb-10">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 mb-3">
              <FileText className="w-3.5 h-3.5" />
              <span>Terms of Service</span>
            </div>
            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-slate-900 dark:text-white tracking-tight">
              Terms & Conditions
            </h1>
            <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">
              Effective Date: {APP_CONFIG.release.releaseDate} · App Package: <code>{APP_CONFIG.app.packageName}</code>
            </p>
          </div>

          {/* Content */}
          <div className="prose prose-slate dark:prose-invert max-w-none space-y-8 text-slate-700 dark:text-slate-300 text-sm sm:text-base leading-relaxed">
            
            <section className="space-y-3">
              <h2 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white">
                1. Acceptance of Terms
              </h2>
              <p>
                By downloading, installing, or using the <strong>{APP_CONFIG.app.name}</strong> Android application (APK), you agree to be bound by these Terms and Conditions. If you do not agree to these terms, do not install or use the application.
              </p>
            </section>

            <section className="space-y-3">
              <h2 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white">
                2. Software License & Distribution
              </h2>
              <p>
                {APP_CONFIG.app.name} grants you a revocable, non-exclusive, non-transferable license to download and install the official APK on authorized Android devices for your personal or commercial business operations.
              </p>
              <p>
                You may not:
              </p>
              <ul className="list-disc pl-5 space-y-1">
                <li>Reverse-engineer, decompile, or disassemble the binary packages without prior written authorization.</li>
                <li>Redistribute modified, re-signed, or tampered copies of the APK claiming them to be official releases.</li>
                <li>Attempt to bypass security features, tenant authentication, or database row-level security policies.</li>
              </ul>
            </section>

            <section className="space-y-3">
              <h2 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white">
                3. APK Authenticity & Verification
              </h2>
              <p>
                Official releases are distributed exclusively through our official website and designated GitHub Releases repository. We provide cryptographic SHA-256 hashes for all builds. We cannot guarantee the security, integrity, or functionality of packages downloaded from unauthorized third-party mirrors.
              </p>
            </section>

            <section className="space-y-3">
              <h2 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white">
                4. Operational Use & Accuracy
              </h2>
              <p>
                {APP_CONFIG.app.name} provides inventory tracking, stock movements, and customer ledger management. While we strive for absolute precision and offline resiliency, business operators are encouraged to perform regular manual stock audits and baseline reconciliations.
              </p>
            </section>

            <section className="space-y-3">
              <h2 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white">
                5. Disclaimer of Warranties
              </h2>
              <p className="uppercase text-xs sm:text-sm text-slate-500 font-medium">
                The application is provided &ldquo;as is&rdquo; and &ldquo;as available&rdquo;, without warranties of any kind, either express or implied, including but not limited to the implied warranties of merchantability, fitness for a particular purpose, and non-infringement.
              </p>
            </section>

            <section className="space-y-3">
              <h2 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white">
                6. Contact Information
              </h2>
              <p>
                For inquiries regarding these Terms and Conditions, please email us at <a href={`mailto:${APP_CONFIG.links.supportEmail}`} className="text-emerald-500 hover:underline">{APP_CONFIG.links.supportEmail}</a>.
              </p>
            </section>

          </div>

        </div>
      </main>

      <Footer />
    </div>
  );
}

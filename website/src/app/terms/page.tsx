import type { Metadata } from "next";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { APP_CONFIG } from "@/config/app.config";

export default function TermsPage() {
  return (
    <div className="flex flex-col min-h-screen bg-[#F7F7F2] dark:bg-[#111714] text-[#202521] dark:text-[#F3F6F1] transition-colors">
      <Navbar />

      <main className="flex-1 py-14 sm:py-20 border-b border-[#E2E5DD] dark:border-[#26372F]">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          
          {/* Back Navigation */}
          <Link
            href="/"
            className="inline-flex items-center gap-2 text-xs font-mono uppercase tracking-wider text-[#777D76] dark:text-[#8E9B93] hover:text-[#173D32] dark:hover:text-[#C8D8B9] mb-8 transition-colors"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Return to Distribution</span>
          </Link>

          {/* Header */}
          <div className="border-b border-[#E2E5DD] dark:border-[#26372F] pb-8 mb-10">
            <div className="flex items-center gap-2 mb-3">
              <span className="text-xs font-mono tracking-widest text-[#777D76] dark:text-[#8E9B93] uppercase">
                Software Distribution Agreement
              </span>
            </div>
            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-normal tracking-tight text-[#202521] dark:text-[#F3F6F1]">
              Terms of <span className="font-semibold text-[#173D32] dark:text-[#C8D8B9]">Service</span>
            </h1>
            <p className="mt-3 text-xs font-mono text-[#777D76] dark:text-[#8E9B93]">
              Effective: {APP_CONFIG.release.releaseDate} · App Package: {APP_CONFIG.app.packageName}
            </p>
          </div>

          {/* Content */}
          <div className="space-y-8 text-sm sm:text-base leading-relaxed text-[#202521] dark:text-[#F3F6F1]">
            
            <section className="space-y-3">
              <h2 className="text-lg font-semibold text-[#202521] dark:text-[#F3F6F1]">
                1. Acceptance of Terms
              </h2>
              <p className="text-[#777D76] dark:text-[#8E9B93]">
                By downloading, installing, or executing the <strong>{APP_CONFIG.app.name}</strong> Android package (APK), you agree to be bound by these Terms of Service. If you do not agree with any provision herein, do not install or use the software.
              </p>
            </section>

            <section className="space-y-3">
              <h2 className="text-lg font-semibold text-[#202521] dark:text-[#F3F6F1]">
                2. Software License &amp; Intellectual Property
              </h2>
              <p className="text-[#777D76] dark:text-[#8E9B93]">
                {APP_CONFIG.app.name} grants authorized users a non-exclusive, revocable license to execute official application binaries for water delivery and stock ledger operations. Users may not decompile, reverse engineer, or redistribute tampered copies of the APK claiming them as official builds.
              </p>
            </section>

            <section className="space-y-3">
              <h2 className="text-lg font-semibold text-[#202521] dark:text-[#F3F6F1]">
                3. Direct Distribution &amp; Integrity Verification
              </h2>
              <p className="text-[#777D76] dark:text-[#8E9B93]">
                Official APK binaries are distributed directly through this website and are accompanied by public SHA-256 cryptographic hashes. Users are responsible for verifying package authenticity when downloading through alternative mirrors.
              </p>
            </section>

            <section className="space-y-3">
              <h2 className="text-lg font-semibold text-[#202521] dark:text-[#F3F6F1]">
                4. Operational Disclaimer
              </h2>
              <p className="text-xs sm:text-sm text-[#777D76] dark:text-[#8E9B93] font-mono uppercase tracking-wider">
                The software is provided &ldquo;as is&rdquo; without warranties of any kind, express or implied. Users should maintain regular operational audits of water can stock baselines and financial receipts.
              </p>
            </section>

            <section className="space-y-3">
              <h2 className="text-lg font-semibold text-[#202521] dark:text-[#F3F6F1]">
                5. Contact
              </h2>
              <p className="text-[#777D76] dark:text-[#8E9B93]">
                For legal inquiries, contact{" "}
                <a href={`mailto:${APP_CONFIG.links.supportEmail}`} className="text-[#173D32] dark:text-[#C8D8B9] font-semibold underline">
                  {APP_CONFIG.links.supportEmail}
                </a>.
              </p>
            </section>

          </div>

        </div>
      </main>

      <Footer />
    </div>
  );
}

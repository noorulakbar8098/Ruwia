import type { Metadata } from "next";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { APP_CONFIG } from "@/config/app.config";

export default function PrivacyPage() {
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
                Legal &amp; Telemetry Disclosure
              </span>
            </div>
            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-normal tracking-tight text-[#202521] dark:text-[#F3F6F1]">
              Privacy <span className="font-semibold text-[#173D32] dark:text-[#C8D8B9]">Policy</span>
            </h1>
            <p className="mt-3 text-xs font-mono text-[#777D76] dark:text-[#8E9B93]">
              Last updated: {APP_CONFIG.release.releaseDate} · Official release package: {APP_CONFIG.app.packageName}
            </p>
          </div>

          {/* Policy Content */}
          <div className="space-y-8 text-sm sm:text-base leading-relaxed text-[#202521] dark:text-[#F3F6F1]">
            
            <section className="space-y-3">
              <h2 className="text-lg font-semibold text-[#202521] dark:text-[#F3F6F1]">
                1. Overview &amp; Telemetry Policy
              </h2>
              <p className="text-[#777D76] dark:text-[#8E9B93]">
                At <strong>{APP_CONFIG.app.name}</strong>, we build dedicated distribution tools for business operations. Our Android application is distributed without third-party advertising SDKs, commercial telemetry beacons, or user tracking networks. We process only the operational information necessary to synchronize stock movements, dispatch drivers, and manage customer accounts.
              </p>
            </section>

            <section className="space-y-4">
              <h2 className="text-lg font-semibold text-[#202521] dark:text-[#F3F6F1]">
                2. Android System Permissions
              </h2>
              <p className="text-[#777D76] dark:text-[#8E9B93]">
                The application requests specific operating system permissions only when required to perform core functions:
              </p>

              <div className="space-y-3">
                <div className="p-5 rounded border border-[#E2E5DD] dark:border-[#26372F] bg-white dark:bg-[#18221D] transition-colors">
                  <div className="font-mono text-xs uppercase tracking-wider text-[#173D32] dark:text-[#C8D8B9] font-semibold mb-1">
                    android.permission.INTERNET
                  </div>
                  <p className="text-xs sm:text-sm text-[#777D76] dark:text-[#8E9B93]">
                    Required to synchronize stock ledgers, customer transactions, and delivery status updates with our secure backend over TLS 1.3 encryption.
                  </p>
                </div>

                <div className="p-5 rounded border border-[#E2E5DD] dark:border-[#26372F] bg-white dark:bg-[#18221D] transition-colors">
                  <div className="font-mono text-xs uppercase tracking-wider text-[#173D32] dark:text-[#C8D8B9] font-semibold mb-1">
                    ACCESS_FINE_LOCATION &amp; FOREGROUND_SERVICE_LOCATION
                  </div>
                  <p className="text-xs sm:text-sm text-[#777D76] dark:text-[#8E9B93]">
                    Used only while delivery drivers actively execute an assigned delivery run. Location tracks drop-off verification and navigation routing, and terminates immediately upon route completion.
                  </p>
                </div>

                <div className="p-5 rounded border border-[#E2E5DD] dark:border-[#26372F] bg-white dark:bg-[#18221D] transition-colors">
                  <div className="font-mono text-xs uppercase tracking-wider text-[#173D32] dark:text-[#C8D8B9] font-semibold mb-1">
                    android.permission.POST_NOTIFICATIONS
                  </div>
                  <p className="text-xs sm:text-sm text-[#777D76] dark:text-[#8E9B93]">
                    Used to deliver instant Firebase Cloud Messaging (FCM) notifications for incoming dispatch tasks and warehouse threshold alerts.
                  </p>
                </div>
              </div>
            </section>

            <section className="space-y-3">
              <h2 className="text-lg font-semibold text-[#202521] dark:text-[#F3F6F1]">
                3. Security &amp; Data Protection
              </h2>
              <p className="text-[#777D76] dark:text-[#8E9B93]">
                All data in transit is protected using TLS 1.3 cryptographic protocols. Databases enforce strict tenant isolation using Row-Level Security (RLS) policies. We do not access contacts, call logs, device storage media, or SMS messages.
              </p>
            </section>

            <section className="space-y-3">
              <h2 className="text-lg font-semibold text-[#202521] dark:text-[#F3F6F1]">
                4. Contact &amp; Inquiries
              </h2>
              <p className="text-[#777D76] dark:text-[#8E9B93]">
                For questions regarding data processing or permission audits, contact our security team at{" "}
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

import type { Metadata } from "next";
import Link from "next/link";
import { ArrowLeft, ShieldCheck, Lock, Smartphone, Database, Mail } from "lucide-react";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { APP_CONFIG } from "@/config/app.config";

export const metadata: Metadata = {
  title: `Privacy Policy - ${APP_CONFIG.app.name}`,
  description: `Official Privacy Policy for the ${APP_CONFIG.app.name} Android application. Learn how we handle your data and device permissions.`,
};

export default function PrivacyPage() {
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
              <ShieldCheck className="w-3.5 h-3.5" />
              <span>Official Privacy Policy</span>
            </div>
            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-slate-900 dark:text-white tracking-tight">
              Privacy Policy
            </h1>
            <p className="mt-3 text-sm text-slate-500 dark:text-slate-400">
              Last updated: {APP_CONFIG.release.releaseDate} · Applies to {APP_CONFIG.app.name} Android Application (package: <code>{APP_CONFIG.app.packageName}</code>)
            </p>
          </div>

          {/* Policy Content */}
          <div className="prose prose-slate dark:prose-invert max-w-none space-y-8 text-slate-700 dark:text-slate-300 text-sm sm:text-base leading-relaxed">
            
            <section className="space-y-3">
              <h2 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
                1. Overview & Commitment
              </h2>
              <p>
                At <strong>{APP_CONFIG.app.name}</strong>, we respect your privacy. This Privacy Policy describes how our Android application collects, uses, and safeguards information when you use our stock ledger, delivery task management, and distribution platform.
              </p>
              <p>
                Unlike many commercial apps, <strong>{APP_CONFIG.app.name} is distributed without third-party advertising networks, analytics tracking beacons, or commercial data brokers</strong>. We collect only what is strictly necessary to perform operational distribution tasks.
              </p>
            </section>

            <section className="space-y-4">
              <h2 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
                2. Android Device Permissions & Usage
              </h2>
              <p>
                When installed on your Android device, {APP_CONFIG.app.name} may request specific operating system permissions. Here is why each permission is required:
              </p>

              <div className="grid grid-cols-1 gap-4 not-prose">
                <div className="p-4 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800">
                  <div className="flex items-center gap-2 font-bold text-slate-900 dark:text-white text-sm mb-1">
                    <Smartphone className="w-4 h-4 text-emerald-500" />
                    <span>Internet & Network Access (<code>android.permission.INTERNET</code>)</span>
                  </div>
                  <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-400">
                    Required to synchronize stock movements, customer bills, and delivery task statuses with your company's secure backend database in real time.
                  </p>
                </div>

                <div className="p-4 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800">
                  <div className="flex items-center gap-2 font-bold text-slate-900 dark:text-white text-sm mb-1">
                    <Smartphone className="w-4 h-4 text-emerald-500" />
                    <span>Location Services (<code>ACCESS_FINE_LOCATION</code> & <code>FOREGROUND_SERVICE_LOCATION</code>)</span>
                  </div>
                  <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-400">
                    Used strictly when an employee is actively running an assigned delivery route. Location coordinates verify drop-off locations and assist in routing navigation. Location is never tracked when the driver is off-duty.
                  </p>
                </div>

                <div className="p-4 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800">
                  <div className="flex items-center gap-2 font-bold text-slate-900 dark:text-white text-sm mb-1">
                    <Smartphone className="w-4 h-4 text-emerald-500" />
                    <span>Push Notifications (<code>android.permission.POST_NOTIFICATIONS</code>)</span>
                  </div>
                  <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-400">
                    Used to alert you of new delivery dispatches, urgent low-stock warnings, and payment confirmations via Firebase Cloud Messaging (FCM). You can toggle notification channels in your device settings at any time.
                  </p>
                </div>
              </div>
            </section>

            <section className="space-y-3">
              <h2 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
                3. Information We Collect
              </h2>
              <p>We process the following operational information:</p>
              <ul className="list-disc pl-5 space-y-1">
                <li><strong>Account Credentials:</strong> Email addresses or employee IDs used to authenticate and assign tenant roles (Admin vs. Employee).</li>
                <li><strong>Transaction & Stock Ledgers:</strong> Quantities of water cans, customer invoices, payment modes, and empty can return records created during normal business operation.</li>
                <li><strong>Device Identifier:</strong> FCM device registration token required to route background push notifications directly to your phone.</li>
              </ul>
              <p>
                We do <strong>not</strong> access your contacts, private photos, camera, microphone, or SMS messages.
              </p>
            </section>

            <section className="space-y-3">
              <h2 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
                4. Data Storage & Security
              </h2>
              <p>
                All data transfers between the Android app and the server use industry-standard Transport Layer Security (TLS 1.3) encryption. Database tables employ Row-Level Security (RLS) policies to prevent unauthorized tenant cross-talk.
              </p>
            </section>

            <section className="space-y-3">
              <h2 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
                5. Contact Us
              </h2>
              <p>
                If you have any questions or requests regarding your data privacy, please contact our administrative team:
              </p>
              <div className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-sm">
                <div className="font-semibold text-slate-900 dark:text-white">{APP_CONFIG.app.name} Team</div>
                <div className="text-slate-600 dark:text-slate-400 mt-1">
                  Email: <a href={`mailto:${APP_CONFIG.links.supportEmail}`} className="text-emerald-500 hover:underline">{APP_CONFIG.links.supportEmail}</a>
                </div>
                <div className="text-slate-600 dark:text-slate-400 mt-0.5">
                  Telegram: <a href={APP_CONFIG.links.telegramSupport} target="_blank" rel="noopener noreferrer" className="text-emerald-500 hover:underline">{APP_CONFIG.links.telegramSupport}</a>
                </div>
              </div>
            </section>

          </div>

        </div>
      </main>

      <Footer />
    </div>
  );
}

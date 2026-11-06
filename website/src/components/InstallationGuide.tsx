"use client";

import { useState } from "react";
import { ChevronDown } from "lucide-react";
import { APP_CONFIG } from "@/config/app.config";

export default function InstallationGuide() {
  const [activeTab, setActiveTab] = useState<"modern" | "mid" | "legacy">("modern");
  const [showTroubleshoot, setShowTroubleshoot] = useState(false);

  const steps = [
    {
      num: "01",
      title: "Tap Download APK",
      description: "Click the primary download button to retrieve the official signed package directly from this website.",
      note: "The download starts immediately. No external portal or GitHub detour.",
    },
    {
      num: "02",
      title: "Wait for Download to Finish",
      description: "Monitor progress in your browser notification tray or download manager until the APK file has downloaded completely.",
      note: "File size is 26.8 MB. Chrome may ask 'File might be harmful' — tap 'Download anyway'.",
    },
    {
      num: "03",
      title: "Open the Downloaded APK",
      description: "Tap the completed download notification, or locate ruwia-v2.4-release.apk inside your device's Downloads directory.",
      note: "Supported by all default Android file managers and browser trays.",
    },
    {
      num: "04",
      title: "Allow Installation Permission",
      description: "If Android requests security permission to install unknown apps, tap Settings, switch on 'Allow from this source', and return.",
      note: "This is a one-time approval per browser or file manager app.",
    },
    {
      num: "05",
      title: "Confirm & Tap Install",
      description: "Review the system package installer dialog and tap Install to finalize the application installation.",
      note: "Existing data and logins are preserved automatically during updates.",
    },
    {
      num: "06",
      title: "Launch & Begin Operations",
      description: "Tap Open to launch Ruwia. Log in with your admin or driver credentials to access live stock and routes.",
      note: "The app icon will now appear on your home screen and drawer.",
    },
  ];

  return (
    <section id="installation" className="py-20 sm:py-28 border-b border-[#E2E5DD] dark:border-[#26372F] transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Eyebrow & Header (No slashes) */}
        <div className="flex items-center gap-2 mb-6">
          <span className="text-xs font-mono tracking-widest text-[#777D76] dark:text-[#8E9B93] uppercase">
            Step-by-Step Procedure
          </span>
        </div>

        <div className="flex flex-col md:flex-row md:items-end justify-between mb-16 pb-6 border-b border-[#E2E5DD] dark:border-[#26372F] gap-6">
          <div>
            <h2 className="text-3xl sm:text-4xl lg:text-5xl font-normal tracking-tight text-[#202521] dark:text-[#F3F6F1]">
              How to install on <br />
              <span className="font-semibold text-[#173D32] dark:text-[#C8D8B9]">your Android device.</span>
            </h2>
          </div>
          <p className="text-sm sm:text-base text-[#777D76] dark:text-[#8E9B93] max-w-sm leading-relaxed">
            Direct APK setup requires less than 30 seconds. Follow the 6 chronological steps below.
          </p>
        </div>

        {/* 6 Step Sequential Ledger Grid (No slashes) */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {steps.map((step) => (
            <div
              key={step.num}
              className="p-8 bg-white dark:bg-[#18221D] border border-[#E2E5DD] dark:border-[#26372F] rounded flex flex-col justify-between transition-colors"
            >
              <div>
                <div className="flex items-center justify-between mb-4">
                  <span className="text-xl font-mono font-bold text-[#173D32] dark:text-[#C8D8B9]">
                    Step {step.num}
                  </span>
                  <span className="text-[10px] font-mono uppercase tracking-wider text-[#777D76] dark:text-[#8E9B93]">
                    Sequential
                  </span>
                </div>
                <h3 className="text-lg font-semibold text-[#202521] dark:text-[#F3F6F1] mb-2">
                  {step.title}
                </h3>
                <p className="text-sm text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
                  {step.description}
                </p>
              </div>

              <div className="mt-6 pt-4 border-t border-[#E2E5DD] dark:border-[#26372F] text-xs font-mono text-[#777D76] dark:text-[#8E9B93]">
                <span>{step.note}</span>
              </div>
            </div>
          ))}
        </div>

        {/* Signing key migration note */}
        <div className="mt-6 border border-[#C8D8B9] dark:border-[#2C3F34] bg-[#EBF0E6] dark:bg-[#202E28] rounded p-5 sm:p-6 transition-colors">
          <div className="text-xs font-mono uppercase tracking-wider text-[#173D32] dark:text-[#C8D8B9] font-semibold mb-2">
            Upgrading from v2.3?
          </div>
          <p className="text-xs sm:text-sm text-[#202521] dark:text-[#F3F6F1] leading-relaxed">
            v2.4 is signed with a new production certificate. If Android reports
            &ldquo;App not installed&rdquo; while updating, uninstall the older v2.3 build
            once, then install v2.4. Your server data stays safe in your account
            and syncs back after you sign in.
          </p>
        </div>

        {/* Android Version Specific Guidance */}
        <div className="mt-14 border border-[#E2E5DD] dark:border-[#26372F] bg-white dark:bg-[#18221D] rounded p-6 sm:p-8 transition-colors">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-[#E2E5DD] dark:border-[#26372F] mb-6">
            <div>
              <h3 className="text-base sm:text-lg font-semibold text-[#202521] dark:text-[#F3F6F1]">
                Version-Specific Permission Instructions
              </h3>
              <p className="text-xs sm:text-sm text-[#777D76] dark:text-[#8E9B93] mt-0.5">
                Android permission layouts vary slightly depending on your installed operating system:
              </p>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={() => setActiveTab("modern")}
                className={`px-3 py-1.5 rounded text-xs font-mono uppercase tracking-wider transition-colors ${
                  activeTab === "modern"
                    ? "bg-[#173D32] text-[#F7F7F2] dark:bg-[#C8D8B9] dark:text-[#111714]"
                    : "bg-[#F7F7F2] dark:bg-[#151E1A] border border-[#E2E5DD] dark:border-[#26372F] text-[#777D76] dark:text-[#8E9B93] hover:text-[#202521] dark:hover:text-[#F3F6F1]"
                }`}
              >
                Android 14–16
              </button>
              <button
                onClick={() => setActiveTab("mid")}
                className={`px-3 py-1.5 rounded text-xs font-mono uppercase tracking-wider transition-colors ${
                  activeTab === "mid"
                    ? "bg-[#173D32] text-[#F7F7F2] dark:bg-[#C8D8B9] dark:text-[#111714]"
                    : "bg-[#F7F7F2] dark:bg-[#151E1A] border border-[#E2E5DD] dark:border-[#26372F] text-[#777D76] dark:text-[#8E9B93] hover:text-[#202521] dark:hover:text-[#F3F6F1]"
                }`}
              >
                Android 8–13
              </button>
              <button
                onClick={() => setActiveTab("legacy")}
                className={`px-3 py-1.5 rounded text-xs font-mono uppercase tracking-wider transition-colors ${
                  activeTab === "legacy"
                    ? "bg-[#173D32] text-[#F7F7F2] dark:bg-[#C8D8B9] dark:text-[#111714]"
                    : "bg-[#F7F7F2] dark:bg-[#151E1A] border border-[#E2E5DD] dark:border-[#26372F] text-[#777D76] dark:text-[#8E9B93] hover:text-[#202521] dark:hover:text-[#F3F6F1]"
                }`}
              >
                Android 7
              </button>
            </div>
          </div>

          <div className="text-sm leading-relaxed text-[#202521] dark:text-[#F3F6F1]">
            {activeTab === "modern" && (
              <div className="space-y-2">
                <div className="font-mono text-xs text-[#173D32] dark:text-[#C8D8B9] uppercase tracking-wider">
                  Android 14, 15, and 16 (API 34–36)
                </div>
                <p className="text-[#777D76] dark:text-[#8E9B93]">
                  {APP_CONFIG.androidVersionNotes.android14Plus}
                </p>
              </div>
            )}

            {activeTab === "mid" && (
              <div className="space-y-2">
                <div className="font-mono text-xs text-[#173D32] dark:text-[#C8D8B9] uppercase tracking-wider">
                  Android 8.0 to Android 13 (API 26–33)
                </div>
                <p className="text-[#777D76] dark:text-[#8E9B93]">
                  {APP_CONFIG.androidVersionNotes.android8to13}
                </p>
              </div>
            )}

            {activeTab === "legacy" && (
              <div className="space-y-2">
                <div className="font-mono text-xs text-[#173D32] dark:text-[#C8D8B9] uppercase tracking-wider">
                  Android 7.0 &amp; 7.1 Nougat (API 24–25)
                </div>
                <p className="text-[#777D76] dark:text-[#8E9B93]">
                  {APP_CONFIG.androidVersionNotes.android7}
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Play Protect Accordion */}
        <div className="mt-6 border border-[#E2E5DD] dark:border-[#26372F] bg-white dark:bg-[#18221D] rounded overflow-hidden transition-colors">
          <button
            onClick={() => setShowTroubleshoot(!showTroubleshoot)}
            type="button"
            className="w-full p-5 text-left flex items-center justify-between hover:bg-[#F7F7F2] dark:hover:bg-[#151E1A] transition-colors"
          >
            <span className="text-sm font-semibold text-[#202521] dark:text-[#F3F6F1]">
              Seeing a Google Play Protect &ldquo;Unrecognized app&rdquo; prompt?
            </span>
            <ChevronDown
              className={`w-4 h-4 text-[#777D76] dark:text-[#8E9B93] transition-transform duration-150 ${
                showTroubleshoot ? "rotate-180" : ""
              }`}
            />
          </button>

          {showTroubleshoot && (
            <div className="p-6 border-t border-[#E2E5DD] dark:border-[#26372F] bg-[#F7F7F2] dark:bg-[#151E1A] text-xs sm:text-sm text-[#777D76] dark:text-[#8E9B93] space-y-2">
              <p>
                Because this release is distributed directly without Google Play Store intermediation, Google Play Protect may display an advisory: <em>&ldquo;Unrecognized app&rdquo;</em>.
              </p>
              <div className="p-3 bg-white dark:bg-[#111714] border border-[#E2E5DD] dark:border-[#26372F] rounded font-mono text-xs text-[#202521] dark:text-[#F3F6F1]">
                Tap <strong>&ldquo;More details&rdquo;</strong>, then tap <strong>&ldquo;Install anyway&rdquo;</strong>.
              </div>
              <p>
                Ruwia is verified clean, ad-free, and cryptographically signed with our company certificate.
              </p>
            </div>
          )}
        </div>

      </div>
    </section>
  );
}

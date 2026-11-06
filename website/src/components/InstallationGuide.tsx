"use client";

import { useState } from "react";
import { Ban, Check, ChevronDown, Zap } from "lucide-react";
import { APP_CONFIG } from "@/config/app.config";
import NotificationShade from "@/components/NotificationShade";

const STEPS = [
  {
    num: "01",
    title: "Tap Download APK",
    description:
      "Use the download button in the header or the specifications section. The signed package is served directly by this website.",
    note: "Chrome may warn that APK files can be harmful. Tap Download anyway — every APK on the web gets that prompt.",
  },
  {
    num: "02",
    title: "Open the download notification",
    description:
      "Swipe down from the top of the screen and tap the completed file. The Ruwia installer opens immediately.",
    note: "This is where most people lose time. The file is in Downloads, but the notification is one tap away.",
  },
  {
    num: "03",
    title: "Allow this source (first install only)",
    description:
      "If Android says installs are blocked, tap Settings inside that prompt, switch on Allow from this source, then press Back.",
    note: "Android returns you to the installer automatically. The approval is remembered for future updates.",
  },
  {
    num: "04",
    title: "Tap Install, then Open",
    description:
      "Confirm the signed package details and tap Install. When it finishes, tap Open to launch Ruwia and sign in.",
    note: "If Play Protect shows Unrecognized app, tap More details, then Install anyway.",
  },
];

const VERSION_TABS = [
  {
    id: "modern",
    label: "Android 14–16",
    heading: "Android 14, 15, and 16 (API 34–36)",
    noteKey: "android14Plus",
  },
  {
    id: "mid",
    label: "Android 8–13",
    heading: "Android 8.0 to Android 13 (API 26–33)",
    noteKey: "android8to13",
  },
  {
    id: "legacy",
    label: "Android 7",
    heading: "Android 7.0 & 7.1 Nougat (API 24–25)",
    noteKey: "android7",
  },
] as const;

type VersionTabId = (typeof VERSION_TABS)[number]["id"];

const ANDROID_RULES = [
  { allowed: true, text: "Download the signed APK to your phone." },
  { allowed: false, text: "Open Android's package installer on your behalf." },
  { allowed: false, text: "Install an app without your confirmation." },
];

export default function InstallationGuide() {
  const [activeTab, setActiveTab] = useState<VersionTabId>("modern");
  const [showTroubleshoot, setShowTroubleshoot] = useState(false);

  const activeTabConfig =
    VERSION_TABS.find((tab) => tab.id === activeTab) ?? VERSION_TABS[0];

  return (
    <section id="installation" className="py-20 sm:py-28 border-b border-[#E2E5DD] dark:border-[#26372F] transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

        {/* Eyebrow & Header (No slashes) */}
        <div className="flex items-center gap-2 mb-6">
          <span className="text-xs font-mono tracking-widest text-[#777D76] dark:text-[#8E9B93] uppercase">
            Step-by-Step Procedure
          </span>
        </div>

        <div className="flex flex-col md:flex-row md:items-end justify-between mb-12 pb-6 border-b border-[#E2E5DD] dark:border-[#26372F] gap-6">
          <div>
            <h2 className="text-3xl sm:text-4xl lg:text-5xl font-normal tracking-tight text-[#202521] dark:text-[#F3F6F1]">
              How to install on <br />
              <span className="font-semibold text-[#173D32] dark:text-[#C8D8B9]">your Android device.</span>
            </h2>
          </div>
          <p className="text-sm sm:text-base text-[#777D76] dark:text-[#8E9B93] max-w-sm leading-relaxed">
            Four short steps. Only the third one appears on the first install —
            updates skip straight to the confirmation.
          </p>
        </div>

        {/* Fastest path + platform rules */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 mb-6">
          <div className="lg:col-span-5 p-6 sm:p-8 bg-white dark:bg-[#18221D] border border-[#E2E5DD] dark:border-[#26372F] rounded transition-colors">
            <div className="flex items-center gap-2 text-[11px] font-mono uppercase tracking-wider text-[#173D32] dark:text-[#C8D8B9] mb-3">
              <Zap className="w-3.5 h-3.5" />
              <span>Fastest path</span>
            </div>
            <h3 className="text-lg font-semibold text-[#202521] dark:text-[#F3F6F1] mb-2">
              Skip the file manager.
            </h3>
            <p className="text-sm text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
              The moment the APK finishes downloading, Android posts a
              notification. That notification opens the installer directly —
              no searching, no Settings detour.
            </p>
            <NotificationShade className="mt-5" />
            <p className="mt-4 text-xs text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
              No notification? Open Chrome and tap the download icon in the
              toolbar, or use Files → Downloads.
            </p>
          </div>

          <div className="lg:col-span-7 p-6 sm:p-8 bg-[#F7F7F2] dark:bg-[#151E1A] border border-[#E2E5DD] dark:border-[#26372F] rounded transition-colors">
            <h3 className="text-lg font-semibold text-[#202521] dark:text-[#F3F6F1] mb-2">
              What Android allows a website to do
            </h3>
            <p className="text-sm text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
              The remaining confirmation is a platform rule, not a Ruwia
              limitation. It exists so no website can install software on your
              phone without your knowledge.
            </p>

            <ul className="mt-5 space-y-3">
              {ANDROID_RULES.map((rule) => (
                <li key={rule.text} className="flex items-start gap-2.5 text-sm text-[#202521] dark:text-[#F3F6F1]">
                  <span
                    className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full ${
                      rule.allowed
                        ? "bg-[#EBF0E6] text-[#173D32] dark:bg-[#202E28] dark:text-[#C8D8B9]"
                        : "bg-[#F1E9E4] text-[#8A4B32] dark:bg-[#2A1F1A] dark:text-[#D9A98F]"
                    }`}
                  >
                    {rule.allowed ? <Check className="w-3 h-3" /> : <Ban className="w-3 h-3" />}
                  </span>
                  <span className="leading-relaxed">{rule.text}</span>
                </li>
              ))}
            </ul>

            <p className="mt-5 pt-4 border-t border-[#E2E5DD] dark:border-[#26372F] text-xs text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
              Only a store install (Google Play) or an organisation&apos;s
              device-management system can skip these prompts. Direct APK
              distribution always ends with one tap from you.
            </p>
          </div>
        </div>

        {/* Sequential step ledger */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {STEPS.map((step) => (
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
            v{APP_CONFIG.release.version} is signed with a new production
            certificate. If Android reports &ldquo;App not installed&rdquo; while
            updating, uninstall the older v2.3 build once, then install
            v{APP_CONFIG.release.version}. Your server data stays safe in your
            account and syncs back after you sign in.
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
                Android moves this toggle around between releases. Pick your version:
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              {VERSION_TABS.map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  type="button"
                  aria-pressed={activeTab === tab.id}
                  className={`px-3 py-1.5 rounded text-xs font-mono uppercase tracking-wider transition-colors ${
                    activeTab === tab.id
                      ? "bg-[#173D32] text-[#F7F7F2] dark:bg-[#C8D8B9] dark:text-[#111714]"
                      : "bg-[#F7F7F2] dark:bg-[#151E1A] border border-[#E2E5DD] dark:border-[#26372F] text-[#777D76] dark:text-[#8E9B93] hover:text-[#202521] dark:hover:text-[#F3F6F1]"
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </div>

          <div className="text-sm leading-relaxed text-[#202521] dark:text-[#F3F6F1] space-y-2">
            <div className="font-mono text-xs text-[#173D32] dark:text-[#C8D8B9] uppercase tracking-wider">
              {activeTabConfig.heading}
            </div>
            <p className="text-[#777D76] dark:text-[#8E9B93]">
              {APP_CONFIG.androidVersionNotes[activeTabConfig.noteKey]}
            </p>
          </div>
        </div>

        {/* Play Protect Accordion */}
        <div className="mt-6 border border-[#E2E5DD] dark:border-[#26372F] bg-white dark:bg-[#18221D] rounded overflow-hidden transition-colors">
          <button
            onClick={() => setShowTroubleshoot(!showTroubleshoot)}
            type="button"
            aria-expanded={showTroubleshoot}
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
              <p>
                If the &ldquo;Allow from this source&rdquo; toggle is greyed out,
                open Settings → Apps → your browser → menu → Allow restricted
                settings, then retry the install.
              </p>
            </div>
          )}
        </div>

      </div>
    </section>
  );
}

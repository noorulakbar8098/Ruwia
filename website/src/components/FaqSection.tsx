"use client";

import { useState } from "react";
import {
  HelpCircle,
  ChevronDown,
  Search,
  Sparkles,
  MessageCircleQuestion,
} from "lucide-react";
import { APP_CONFIG, FaqItem } from "@/config/app.config";

export default function FaqSection() {
  const [openIndexes, setOpenIndexes] = useState<number[]>([0, 3]); // First and install questions open by default
  const [searchQuery, setSearchQuery] = useState("");

  const toggleAccordion = (index: number) => {
    setOpenIndexes((prev) =>
      prev.includes(index) ? prev.filter((i) => i !== index) : [...prev, index]
    );
  };

  const filteredFaqs = APP_CONFIG.faqs.filter(
    (faq: FaqItem) =>
      faq.question.toLowerCase().includes(searchQuery.toLowerCase()) ||
      faq.answer.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <section id="faq" className="py-20 sm:py-28 relative bg-slate-50/50 dark:bg-slate-900/20 border-t border-slate-200/60 dark:border-slate-800/60">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-12 sm:mb-16">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 mb-4">
            <HelpCircle className="w-3.5 h-3.5" />
            <span>Got Questions?</span>
          </div>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-slate-900 dark:text-white tracking-tight">
            Frequently Asked Questions
          </h2>
          <p className="mt-4 text-base sm:text-lg text-slate-600 dark:text-slate-400">
            Everything you need to know about downloading, installing, updating, and trusting the Ruwia Android APK.
          </p>

          {/* Search Filter */}
          <div className="mt-8 relative max-w-md mx-auto">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search questions (e.g. Google Play, update, safe)..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-sm text-slate-900 dark:text-white placeholder:text-slate-400 focus:outline-none focus:border-emerald-500 shadow-sm"
            />
          </div>
        </div>

        {/* FAQs Accordion */}
        <div className="space-y-4">
          {filteredFaqs.length === 0 ? (
            <div className="p-8 text-center text-slate-500 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800">
              No matching questions found for &ldquo;{searchQuery}&rdquo;. Try another search term or contact our support team.
            </div>
          ) : (
            filteredFaqs.map((faq: FaqItem, idx: number) => {
              const isOpen = openIndexes.includes(idx);
              return (
                <div
                  key={idx}
                  className={`rounded-2xl border transition-all duration-200 overflow-hidden ${
                    isOpen
                      ? "border-emerald-500/40 bg-white dark:bg-slate-900 shadow-md shadow-emerald-950/5"
                      : "border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/60 hover:border-slate-300 dark:hover:border-slate-700"
                  }`}
                >
                  <button
                    onClick={() => toggleAccordion(idx)}
                    type="button"
                    className="w-full p-5 sm:p-6 text-left flex items-center justify-between gap-4 select-none focus:outline-none"
                    aria-expanded={isOpen}
                  >
                    <span className="text-base sm:text-lg font-bold text-slate-900 dark:text-white">
                      {faq.question}
                    </span>
                    <div
                      className={`flex items-center justify-center w-8 h-8 rounded-full transition-transform duration-200 shrink-0 ${
                        isOpen
                          ? "bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 rotate-180"
                          : "bg-slate-100 dark:bg-slate-800 text-slate-400"
                      }`}
                    >
                      <ChevronDown className="w-4 h-4" />
                    </div>
                  </button>

                  {isOpen && (
                    <div className="px-5 pb-6 sm:px-6 pt-0 text-sm sm:text-base text-slate-600 dark:text-slate-300 leading-relaxed border-t border-slate-100 dark:border-slate-800/60 pt-4 animate-in fade-in duration-150">
                      {faq.answer}
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>

        {/* Still Have Questions Banner */}
        <div className="mt-12 p-6 rounded-3xl bg-emerald-500/10 border border-emerald-500/20 text-center flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3 text-left">
            <div className="w-10 h-10 rounded-xl bg-emerald-500/20 flex items-center justify-center text-emerald-600 dark:text-emerald-400 shrink-0">
              <MessageCircleQuestion className="w-5 h-5" />
            </div>
            <div>
              <div className="text-sm font-bold text-slate-900 dark:text-white">
                Have a different question or need direct help?
              </div>
              <div className="text-xs text-slate-600 dark:text-slate-400">
                Our support team is available via email and Telegram.
              </div>
            </div>
          </div>
          <a
            href={`mailto:${APP_CONFIG.links.supportEmail}`}
            className="px-4 py-2 rounded-xl text-xs sm:text-sm font-bold bg-emerald-500 text-slate-950 hover:bg-emerald-400 transition-colors shrink-0"
          >
            Contact Support
          </a>
        </div>

      </div>
    </section>
  );
}

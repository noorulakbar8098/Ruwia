"use client";

import { useState } from "react";
import { ChevronDown, Search } from "lucide-react";
import { APP_CONFIG, FaqItem } from "@/config/app.config";

export default function FaqSection() {
  const [openIndexes, setOpenIndexes] = useState<number[]>([0, 2]);
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
    <section id="faq" className="py-20 sm:py-28 border-b border-[#E2E5DD] dark:border-[#26372F] transition-colors">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Eyebrow & Header (No slashes) */}
        <div className="flex items-center gap-2 mb-6">
          <span className="text-xs font-mono tracking-widest text-[#777D76] dark:text-[#8E9B93] uppercase">
            Questions &amp; Answers
          </span>
        </div>

        <div className="flex flex-col md:flex-row md:items-end justify-between mb-12 pb-6 border-b border-[#E2E5DD] dark:border-[#26372F] gap-6">
          <div>
            <h2 className="text-3xl sm:text-4xl lg:text-5xl font-normal tracking-tight text-[#202521] dark:text-[#F3F6F1]">
              Frequently asked <br />
              <span className="font-semibold text-[#173D32] dark:text-[#C8D8B9]">questions.</span>
            </h2>
          </div>

          {/* Minimal Search Input */}
          <div className="relative w-full md:w-72">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-[#777D76] dark:text-[#8E9B93]" />
            <input
              type="text"
              placeholder="Filter topics..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-2 bg-white dark:bg-[#18221D] border border-[#E2E5DD] dark:border-[#26372F] rounded text-xs font-mono text-[#202521] dark:text-[#F3F6F1] placeholder:text-[#777D76] dark:placeholder:text-[#8E9B93] focus:outline-none focus:border-[#173D32] dark:focus:border-[#C8D8B9]"
            />
          </div>
        </div>

        {/* Minimalist Accordion List with Hairline Borders */}
        <div className="divide-y divide-[#E2E5DD] dark:divide-[#26372F] border-t border-b border-[#E2E5DD] dark:border-[#26372F]">
          {filteredFaqs.length === 0 ? (
            <div className="py-12 text-center text-xs font-mono text-[#777D76] dark:text-[#8E9B93]">
              No questions matched &ldquo;{searchQuery}&rdquo;. Contact our team at {APP_CONFIG.links.supportEmail}.
            </div>
          ) : (
            filteredFaqs.map((faq: FaqItem, idx: number) => {
              const isOpen = openIndexes.includes(idx);
              return (
                <div key={idx} className="transition-colors">
                  <button
                    onClick={() => toggleAccordion(idx)}
                    type="button"
                    className="w-full py-5 text-left flex items-center justify-between gap-4 select-none focus:outline-none group"
                    aria-expanded={isOpen}
                  >
                    <span className="text-base sm:text-lg font-medium text-[#202521] dark:text-[#F3F6F1] group-hover:text-[#173D32] dark:group-hover:text-[#C8D8B9] transition-colors">
                      {faq.question}
                    </span>
                    <ChevronDown
                      className={`w-4 h-4 text-[#777D76] dark:text-[#8E9B93] transition-transform duration-150 shrink-0 ${
                        isOpen ? "rotate-180 text-[#173D32] dark:text-[#C8D8B9]" : ""
                      }`}
                    />
                  </button>

                  {isOpen && (
                    <div className="pb-6 text-sm text-[#777D76] dark:text-[#8E9B93] leading-relaxed max-w-3xl">
                      {faq.answer}
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>

        {/* Support Help Line */}
        <div className="mt-10 flex flex-col sm:flex-row sm:items-center justify-between gap-4 text-xs font-mono text-[#777D76] dark:text-[#8E9B93]">
          <span>Need technical assistance or custom deployment?</span>
          <a
            href={`mailto:${APP_CONFIG.links.supportEmail}`}
            className="text-[#173D32] dark:text-[#C8D8B9] hover:underline font-semibold"
          >
            Contact support ({APP_CONFIG.links.supportEmail}) →
          </a>
        </div>

      </div>
    </section>
  );
}

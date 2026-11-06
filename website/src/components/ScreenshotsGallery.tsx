"use client";

import { useState, useEffect, useCallback } from "react";
import Image from "next/image";
import { Maximize2, X, ChevronLeft, ChevronRight } from "lucide-react";
import { APP_CONFIG, ScreenshotItem } from "@/config/app.config";

export default function ScreenshotsGallery() {
  const [selectedCategory, setSelectedCategory] = useState<string>("All");
  const [activeLightboxIndex, setActiveLightboxIndex] = useState<number | null>(null);

  const categories = ["All", "Dashboard", "Inventory", "Deliveries", "Billing"];

  const filteredScreenshots =
    selectedCategory === "All"
      ? APP_CONFIG.screenshots
      : APP_CONFIG.screenshots.filter((s) => s.category === selectedCategory);

  const handleNext = useCallback(() => {
    if (activeLightboxIndex === null) return;
    setActiveLightboxIndex((prev) =>
      prev !== null ? (prev + 1) % APP_CONFIG.screenshots.length : 0
    );
  }, [activeLightboxIndex]);

  const handlePrev = useCallback(() => {
    if (activeLightboxIndex === null) return;
    setActiveLightboxIndex((prev) =>
      prev !== null
        ? (prev - 1 + APP_CONFIG.screenshots.length) % APP_CONFIG.screenshots.length
        : 0
    );
  }, [activeLightboxIndex]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (activeLightboxIndex === null) return;
      if (e.key === "Escape") setActiveLightboxIndex(null);
      if (e.key === "ArrowRight") handleNext();
      if (e.key === "ArrowLeft") handlePrev();
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [activeLightboxIndex, handleNext, handlePrev]);

  useEffect(() => {
    if (activeLightboxIndex !== null) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
    return () => {
      document.body.style.overflow = "";
    };
  }, [activeLightboxIndex]);

  return (
    <section id="screenshots" className="py-20 sm:py-28 border-b border-[#E2E5DD] dark:border-[#26372F] transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Eyebrow & Header (No slashes) */}
        <div className="flex items-center gap-2 mb-6">
          <span className="text-xs font-mono tracking-widest text-[#777D76] dark:text-[#8E9B93] uppercase">
            Interface &amp; Workflows
          </span>
        </div>

        <div className="flex flex-col md:flex-row md:items-end justify-between mb-12 pb-6 border-b border-[#E2E5DD] dark:border-[#26372F] gap-6">
          <div>
            <h2 className="text-3xl sm:text-4xl lg:text-5xl font-normal tracking-tight text-[#202521] dark:text-[#F3F6F1]">
              Designed for tactile <br />
              <span className="font-semibold text-[#173D32] dark:text-[#C8D8B9]">efficiency in the field.</span>
            </h2>
          </div>

          {/* Minimalist Category Filter Buttons */}
          <div className="flex flex-wrap items-center gap-2">
            {categories.map((cat) => (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                type="button"
                className={`px-3 py-1.5 rounded text-xs font-mono uppercase tracking-wider transition-colors ${
                  selectedCategory === cat
                    ? "bg-[#173D32] text-[#F7F7F2] dark:bg-[#C8D8B9] dark:text-[#111714]"
                    : "bg-white dark:bg-[#18221D] border border-[#E2E5DD] dark:border-[#26372F] text-[#777D76] dark:text-[#8E9B93] hover:text-[#202521] dark:hover:text-[#F3F6F1] hover:border-[#777D76] dark:hover:border-[#8E9B93]"
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        {/* Product Gallery Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
          {filteredScreenshots.map((screen: ScreenshotItem) => {
            const originalIndex = APP_CONFIG.screenshots.findIndex(
              (s) => s.id === screen.id
            );
            return (
              <div
                key={screen.id}
                className="group flex flex-col"
              >
                {/* Clean Realistic Smartphone Presentation */}
                <div
                  onClick={() => setActiveLightboxIndex(originalIndex)}
                  className="cursor-pointer relative w-full drop-shadow-[0_10px_25px_rgba(0,0,0,0.12)] dark:drop-shadow-[0_15px_30px_rgba(0,0,0,0.55)] transition-all duration-200 group-hover:-translate-y-1.5 group-hover:drop-shadow-[0_20px_35px_rgba(0,0,0,0.22)] dark:group-hover:drop-shadow-[0_25px_45px_rgba(0,0,0,0.75)]"
                >
                  <Image
                    src={screen.imageUrl}
                    alt={screen.alt}
                    width={1469}
                    height={3104}
                    className="w-full h-auto object-contain"
                  />

                  {/* Inspect Badge on Hover */}
                  <div className="absolute inset-0 bg-[#173D32]/10 dark:bg-black/20 opacity-0 group-hover:opacity-100 transition-opacity duration-150 rounded-[40px] flex items-center justify-center pointer-events-none">
                    <div className="w-10 h-10 rounded-full bg-[#173D32] dark:bg-[#C8D8B9] text-[#F7F7F2] dark:text-[#111714] flex items-center justify-center shadow-lg">
                      <Maximize2 className="w-4 h-4" />
                    </div>
                  </div>
                </div>

                {/* Editorial Caption (No slashes) */}
                <div className="mt-4 pt-3 border-t border-[#E2E5DD] dark:border-[#26372F]">
                  <div className="flex items-center justify-between text-[11px] font-mono text-[#777D76] dark:text-[#8E9B93] uppercase">
                    <span>{screen.category}</span>
                    <span>Screen {originalIndex + 1}</span>
                  </div>
                  <h3 className="mt-1 text-sm font-semibold text-[#202521] dark:text-[#F3F6F1]">
                    {screen.title}
                  </h3>
                  <p className="mt-1 text-xs text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
                    {screen.description}
                  </p>
                </div>
              </div>
            );
          })}
        </div>

      </div>

      {/* Lightbox Modal */}
      {activeLightboxIndex !== null && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-[#202521]/90 dark:bg-black/90 backdrop-blur-sm p-4 animate-in fade-in duration-150"
          onClick={() => setActiveLightboxIndex(null)}
        >
          <div
            className="relative max-w-xl w-full flex flex-col items-center"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Close Button */}
            <button
              onClick={() => setActiveLightboxIndex(null)}
              type="button"
              className="absolute -top-12 right-0 p-2 rounded text-[#F7F7F2] hover:bg-white/10 transition-colors"
              aria-label="Close lightbox"
            >
              <X className="w-6 h-6" />
            </button>

            {/* Prev / Next controls */}
            <button
              onClick={handlePrev}
              type="button"
              className="absolute left-2 -translate-x-full top-1/2 -translate-y-1/2 p-3 text-white hover:bg-white/10 rounded transition-colors hidden sm:block"
              aria-label="Previous image"
            >
              <ChevronLeft className="w-8 h-8" />
            </button>

            <button
              onClick={handleNext}
              type="button"
              className="absolute right-2 translate-x-full top-1/2 -translate-y-1/2 p-3 text-white hover:bg-white/10 rounded transition-colors hidden sm:block"
              aria-label="Next image"
            >
              <ChevronRight className="w-8 h-8" />
            </button>

            {/* Enlarged Natural Phone Frame */}
            <div className="relative w-[280px] sm:w-[350px] drop-shadow-[0_25px_60px_rgba(0,0,0,0.85)] flex items-center justify-center">
              <Image
                src={APP_CONFIG.screenshots[activeLightboxIndex].imageUrl}
                alt={APP_CONFIG.screenshots[activeLightboxIndex].alt}
                width={1469}
                height={3104}
                className="w-full h-auto max-h-[75vh] object-contain"
                priority
              />
            </div>

            {/* Information Bar */}
            <div className="mt-4 text-center max-w-sm">
              <div className="text-[11px] font-mono uppercase tracking-widest text-[#C8D8B9]">
                {APP_CONFIG.screenshots[activeLightboxIndex].category} · Screen {activeLightboxIndex + 1} of {APP_CONFIG.screenshots.length}
              </div>
              <h4 className="text-base font-semibold text-white mt-1">
                {APP_CONFIG.screenshots[activeLightboxIndex].title}
              </h4>
              <p className="text-xs text-white/70 mt-1">
                {APP_CONFIG.screenshots[activeLightboxIndex].description}
              </p>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

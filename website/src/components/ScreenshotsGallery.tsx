"use client";

import { useState, useEffect, useCallback } from "react";
import Image from "next/image";
import {
  Maximize2,
  X,
  ChevronLeft,
  ChevronRight,
  Layers,
} from "lucide-react";
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
    <section id="screenshots" className="py-20 sm:py-28 relative bg-slate-50/50 dark:bg-slate-900/20 border-y border-slate-200/60 dark:border-slate-800/60">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-12 sm:mb-16">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 mb-4">
            <Layers className="w-3.5 h-3.5" />
            <span>Interactive Mobile Previews</span>
          </div>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-slate-900 dark:text-white tracking-tight">
            See Ruwia in Action
          </h2>
          <p className="mt-4 text-base sm:text-lg text-slate-600 dark:text-slate-400">
            A fluid, native Android application built for warehouse operators, delivery drivers, and distributors. Tap any preview to inspect full size.
          </p>

          {/* Category Filter Tabs */}
          <div className="mt-8 flex flex-wrap items-center justify-center gap-2">
            {categories.map((cat) => (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                type="button"
                className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all duration-200 ${
                  selectedCategory === cat
                    ? "bg-emerald-500 text-slate-950 shadow-md shadow-emerald-500/20"
                    : "bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400 hover:text-emerald-500"
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        {/* Screenshots Grid with Seamless Phone Bezels */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
          {filteredScreenshots.map((screen: ScreenshotItem) => {
            const originalIndex = APP_CONFIG.screenshots.findIndex(
              (s) => s.id === screen.id
            );
            return (
              <div
                key={screen.id}
                className="group flex flex-col items-center"
              >
                {/* Phone Container */}
                <div
                  onClick={() => setActiveLightboxIndex(originalIndex)}
                  className="cursor-pointer relative w-full max-w-[270px] aspect-[390/844] rounded-[44px] p-2 bg-gradient-to-b from-slate-700 via-slate-800 to-slate-900 shadow-xl shadow-slate-950/40 border border-slate-700/80 transition-all duration-300 group-hover:-translate-y-2 group-hover:border-emerald-500/50 group-hover:shadow-2xl group-hover:shadow-emerald-950/30 ring-1 ring-white/5"
                >
                  {/* Screen preview */}
                  <div className="relative w-full h-full rounded-[36px] overflow-hidden bg-[#090D16]">
                    <Image
                      src={screen.imageUrl}
                      alt={screen.alt}
                      fill
                      sizes="270px"
                      className="object-contain transition-transform duration-500 group-hover:scale-105"
                    />

                    {/* Hover Overlay with Enlarge Icon */}
                    <div className="absolute inset-0 bg-slate-950/40 opacity-0 group-hover:opacity-100 backdrop-blur-[2px] transition-opacity duration-200 flex items-center justify-center">
                      <div className="w-12 h-12 rounded-full bg-emerald-400 text-slate-950 flex items-center justify-center shadow-lg transform scale-75 group-hover:scale-100 transition-transform duration-200">
                        <Maximize2 className="w-5 h-5" />
                      </div>
                    </div>
                  </div>
                </div>

                {/* Caption Details */}
                <div className="mt-5 text-center max-w-[270px]">
                  <span className="inline-block px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 mb-1.5">
                    {screen.category}
                  </span>
                  <h3 className="text-base font-bold text-slate-900 dark:text-white">
                    {screen.title}
                  </h3>
                  <p className="mt-1 text-xs text-slate-500 dark:text-slate-400 line-clamp-2">
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
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/90 backdrop-blur-md p-4 sm:p-6 animate-in fade-in duration-200"
          onClick={() => setActiveLightboxIndex(null)}
        >
          <div
            className="relative max-w-4xl w-full flex flex-col items-center"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Close Button */}
            <button
              onClick={() => setActiveLightboxIndex(null)}
              type="button"
              className="absolute -top-12 right-0 sm:right-2 p-2 rounded-full bg-slate-800 text-slate-300 hover:text-white hover:bg-slate-700 transition-colors"
              aria-label="Close preview"
            >
              <X className="w-6 h-6" />
            </button>

            {/* Prev / Next controls */}
            <button
              onClick={handlePrev}
              type="button"
              className="absolute left-2 sm:-left-12 top-1/2 -translate-y-1/2 p-3 rounded-full bg-slate-900/80 hover:bg-slate-800 text-white border border-slate-700 transition-colors z-20 shadow-xl"
              aria-label="Previous screenshot"
            >
              <ChevronLeft className="w-6 h-6" />
            </button>

            <button
              onClick={handleNext}
              type="button"
              className="absolute right-2 sm:-right-12 top-1/2 -translate-y-1/2 p-3 rounded-full bg-slate-900/80 hover:bg-slate-800 text-white border border-slate-700 transition-colors z-20 shadow-xl"
              aria-label="Next screenshot"
            >
              <ChevronRight className="w-6 h-6" />
            </button>

            {/* Enlarged Phone View */}
            <div className="relative w-[300px] sm:w-[340px] aspect-[390/844] rounded-[50px] p-2.5 bg-gradient-to-b from-slate-700 via-slate-800 to-slate-900 shadow-2xl border border-slate-700/80 ring-2 ring-emerald-500/40">
              <div className="relative w-full h-full rounded-[40px] overflow-hidden bg-[#090D16]">
                <Image
                  src={APP_CONFIG.screenshots[activeLightboxIndex].imageUrl}
                  alt={APP_CONFIG.screenshots[activeLightboxIndex].alt}
                  fill
                  sizes="340px"
                  className="object-contain"
                />
              </div>
            </div>

            {/* Lightbox Information Bar */}
            <div className="mt-4 text-center max-w-md">
              <div className="text-emerald-400 text-xs font-bold uppercase tracking-wider">
                {APP_CONFIG.screenshots[activeLightboxIndex].category} · Screen {activeLightboxIndex + 1} of {APP_CONFIG.screenshots.length}
              </div>
              <h4 className="text-xl font-bold text-white mt-1">
                {APP_CONFIG.screenshots[activeLightboxIndex].title}
              </h4>
              <p className="text-sm text-slate-300 mt-1">
                {APP_CONFIG.screenshots[activeLightboxIndex].description}
              </p>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

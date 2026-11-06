"use client";

import {
  Boxes,
  Truck,
  RotateCcw,
  Receipt,
  BellRing,
  WifiOff,
  LucideIcon,
  Sparkles,
} from "lucide-react";
import { APP_CONFIG, FeatureItem } from "@/config/app.config";

const iconMap: Record<string, LucideIcon> = {
  Boxes,
  Truck,
  RotateCcw,
  Receipt,
  BellRing,
  WifiOff,
};

export default function Features() {
  return (
    <section id="features" className="py-20 sm:py-28 relative">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-16 sm:mb-20">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 mb-4">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Built for High-Velocity Operations</span>
          </div>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-slate-900 dark:text-white tracking-tight">
            Designed to Run Your Water Can Business Seamlessly
          </h2>
          <p className="mt-4 text-base sm:text-lg text-slate-600 dark:text-slate-400">
            From warehouse stock counts and empty can returns to GPS delivery dispatch and customer ledgers—everything in one lightning-fast Android application.
          </p>
        </div>

        {/* Features Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 lg:gap-8">
          {APP_CONFIG.features.map((feature: FeatureItem) => {
            const Icon = iconMap[feature.iconName] || Boxes;
            return (
              <div
                key={feature.id}
                className="group relative p-7 sm:p-8 rounded-3xl border border-slate-200 dark:border-slate-800/80 bg-white dark:bg-slate-900/50 hover:bg-slate-50 dark:hover:bg-slate-900 transition-all duration-300 hover:border-emerald-500/40 hover:shadow-xl hover:shadow-emerald-950/5 flex flex-col justify-between"
              >
                {/* Glow on hover */}
                <div className="absolute top-0 right-0 w-32 h-32 bg-emerald-500/5 rounded-full blur-2xl group-hover:bg-emerald-500/10 transition-colors pointer-events-none" />

                <div>
                  {/* Icon & Category Badge */}
                  <div className="flex items-center justify-between mb-6">
                    <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 dark:bg-emerald-500/15 border border-emerald-500/20 flex items-center justify-center text-emerald-600 dark:text-emerald-400 group-hover:scale-110 transition-transform duration-300">
                      <Icon className="w-6 h-6" />
                    </div>

                    {feature.badge && (
                      <span className="text-[11px] font-semibold uppercase tracking-wider px-2.5 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 border border-slate-200 dark:border-slate-700/60">
                        {feature.badge}
                      </span>
                    )}
                  </div>

                  {/* Title */}
                  <h3 className="text-xl font-bold text-slate-900 dark:text-white mb-2.5 tracking-tight group-hover:text-emerald-500 dark:group-hover:text-emerald-400 transition-colors">
                    {feature.title}
                  </h3>

                  {/* Description */}
                  <p className="text-sm sm:text-base text-slate-600 dark:text-slate-400 leading-relaxed">
                    {feature.description}
                  </p>
                </div>

                {/* Subtle border bottom highlight */}
                <div className="mt-6 pt-4 border-t border-slate-100 dark:border-slate-800/60 flex items-center text-xs font-semibold text-emerald-600 dark:text-emerald-400 opacity-0 group-hover:opacity-100 transition-opacity">
                  <span>Included in v{APP_CONFIG.release.version}</span>
                  <span className="ml-1">→</span>
                </div>
              </div>
            );
          })}
        </div>

      </div>
    </section>
  );
}

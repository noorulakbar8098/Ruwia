import Image from "next/image";
import {
  Boxes,
  Truck,
  RotateCcw,
  Receipt,
  BellRing,
  WifiOff,
} from "lucide-react";
import { APP_CONFIG } from "@/config/app.config";

export default function Features() {
  return (
    <section id="features" className="py-20 sm:py-28 border-b border-[#E2E5DD] dark:border-[#26372F] transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">

        {/* Eyebrow & Header (No slashes) */}
        <div className="flex items-center gap-2 mb-6">
          <span className="text-xs font-mono tracking-widest text-[#777D76] dark:text-[#8E9B93] uppercase">
            Capabilities &amp; Architecture
          </span>
        </div>

        <div className="flex flex-col md:flex-row md:items-end justify-between mb-16 pb-6 border-b border-[#E2E5DD] dark:border-[#26372F] gap-6">
          <div className="max-w-2xl">
            <h2 className="text-3xl sm:text-4xl lg:text-5xl font-normal tracking-tight text-[#202521] dark:text-[#F3F6F1]">
              Engineered for daily operations <br />
              <span className="font-semibold text-[#173D32] dark:text-[#C8D8B9]">on the move.</span>
            </h2>
          </div>
          <p className="text-sm sm:text-base text-[#777D76] dark:text-[#8E9B93] max-w-md leading-relaxed">
            Every feature is purpose-built for physical water distribution: inventory auditability, rapid POS checkout, and seamless driver dispatch.
          </p>
        </div>

        {/* Large Featured Capability Area: Stock Engine Showcase */}
        <div className="mb-14 border border-[#E2E5DD] dark:border-[#26372F] bg-white dark:bg-[#18221D] rounded p-8 sm:p-12 transition-colors">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-center">

            {/* Left Description & Metrics */}
            <div className="lg:col-span-7 space-y-6">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded bg-[#EBF0E6] dark:bg-[#202E28] text-[#173D32] dark:text-[#C8D8B9] text-xs font-mono uppercase tracking-wider">
                <Boxes className="w-3.5 h-3.5" strokeWidth={1.5} />
                <span>Primary Capability · Core Ledger</span>
              </div>

              <h3 className="text-2xl sm:text-3xl font-semibold text-[#202521] dark:text-[#F3F6F1] tracking-tight">
                Live Multi-Warehouse Stock &amp; Can Tracking
              </h3>

              <p className="text-sm sm:text-base text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
                Track every 20L water can, empty return case, and buffer stock across multiple hub locations in real time. Drivers record drop-offs and collect empties in one tap, instantly reconciling warehouse ledgers.
              </p>

              {/* Technical Capability Specs */}
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 pt-2 border-t border-[#E2E5DD] dark:border-[#26372F]">
                <div>
                  <div className="text-[10px] font-mono tracking-wider text-[#777D76] dark:text-[#8E9B93] uppercase">Ledger Sync</div>
                  <div className="mt-1 text-sm font-semibold text-[#202521] dark:text-[#F3F6F1]">Zero Latency</div>
                </div>
                <div>
                  <div className="text-[10px] font-mono tracking-wider text-[#777D76] dark:text-[#8E9B93] uppercase">Audit History</div>
                  <div className="mt-1 text-sm font-semibold text-[#202521] dark:text-[#F3F6F1]">Immutable Log</div>
                </div>
                <div>
                  <div className="text-[10px] font-mono tracking-wider text-[#777D76] dark:text-[#8E9B93] uppercase">Shop Tenancy</div>
                  <div className="mt-1 text-sm font-semibold text-[#202521] dark:text-[#F3F6F1]">Isolated Chips</div>
                </div>
              </div>
            </div>

            {/* Right Screen Snippet */}
            <div className="lg:col-span-5 flex justify-center">
              <div className="relative w-full max-w-[280px] drop-shadow-[0_20px_35px_rgba(0,0,0,0.18)] dark:drop-shadow-[0_25px_45px_rgba(0,0,0,0.7)] transition-transform duration-300 hover:scale-[1.01]">
                <Image
                  src={APP_CONFIG.app.featureScreenshotPath}
                  alt={`${APP_CONFIG.app.name} Live Multi-Shop Inventory`}
                  width={1469}
                  height={3104}
                  className="w-full h-auto object-contain"
                />
              </div>
            </div>

          </div>
        </div>

        {/* Secondary Asymmetric Features Grid (No slashes in badges) */}
        <div className="grid grid-cols-1 md:grid-cols-12 gap-6">

          {/* Card 1 (Span 7): GPS Delivery Dispatch */}
          <div className="md:col-span-7 p-8 bg-white dark:bg-[#18221D] border border-[#E2E5DD] dark:border-[#26372F] rounded flex flex-col justify-between transition-colors">
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-10 h-10 rounded border border-[#E2E5DD] dark:border-[#26372F] bg-[#F7F7F2] dark:bg-[#111714] flex items-center justify-center text-[#173D32] dark:text-[#C8D8B9]">
                  <Truck className="w-5 h-5" strokeWidth={1.5} />
                </div>
                <span className="text-[11px] font-mono uppercase tracking-wider text-[#777D76] dark:text-[#8E9B93]">
                  Logistics · Capability 02
                </span>
              </div>
              <h4 className="text-xl font-semibold text-[#202521] dark:text-[#F3F6F1] mb-2">
                Route Dispatch &amp; GPS Navigation
              </h4>
              <p className="text-sm text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
                Assign bulk orders to delivery personnel, launch Google Maps directions in one tap, and log customer signatures or photo drop-off verifications on-site.
              </p>
            </div>
            <div className="mt-6 pt-4 border-t border-[#E2E5DD] dark:border-[#26372F] flex items-center justify-between text-xs font-mono text-[#777D76] dark:text-[#8E9B93]">
              <span>Background Service Enabled</span>
              <span className="text-[#173D32] dark:text-[#C8D8B9] font-semibold">Ready in v{APP_CONFIG.release.version}</span>
            </div>
          </div>

          {/* Card 2 (Span 5): Customer Can Deposits */}
          <div className="md:col-span-5 p-8 bg-white dark:bg-[#18221D] border border-[#E2E5DD] dark:border-[#26372F] rounded flex flex-col justify-between transition-colors">
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-10 h-10 rounded border border-[#E2E5DD] dark:border-[#26372F] bg-[#F7F7F2] dark:bg-[#111714] flex items-center justify-center text-[#173D32] dark:text-[#C8D8B9]">
                  <RotateCcw className="w-5 h-5" strokeWidth={1.5} />
                </div>
                <span className="text-[11px] font-mono uppercase tracking-wider text-[#777D76] dark:text-[#8E9B93]">
                  Deposits · Capability 03
                </span>
              </div>
              <h4 className="text-xl font-semibold text-[#202521] dark:text-[#F3F6F1] mb-2">
                Can Ledger &amp; Deposit Balances
              </h4>
              <p className="text-sm text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
                Never lose a deposit again. Automatically track outstanding 20L can counts per customer account with clear return histories.
              </p>
            </div>
            <div className="mt-6 pt-4 border-t border-[#E2E5DD] dark:border-[#26372F] text-xs font-mono text-[#777D76] dark:text-[#8E9B93]">
              <span>Automated bottle reconciliation</span>
            </div>
          </div>

          {/* Card 3 (Span 4): POS Billing & Receipts */}
          <div className="md:col-span-4 p-8 bg-white dark:bg-[#18221D] border border-[#E2E5DD] dark:border-[#26372F] rounded flex flex-col justify-between transition-colors">
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-10 h-10 rounded border border-[#E2E5DD] dark:border-[#26372F] bg-[#F7F7F2] dark:bg-[#111714] flex items-center justify-center text-[#173D32] dark:text-[#C8D8B9]">
                  <Receipt className="w-5 h-5" strokeWidth={1.5} />
                </div>
                <span className="text-[11px] font-mono uppercase tracking-wider text-[#777D76] dark:text-[#8E9B93]">
                  Billing · Capability 04
                </span>
              </div>
              <h4 className="text-lg font-semibold text-[#202521] dark:text-[#F3F6F1] mb-2">
                POS Sales &amp; Instant Receipts
              </h4>
              <p className="text-sm text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
                Generate clean invoices, accept cash or UPI, and share receipts instantly via WhatsApp or SMS.
              </p>
            </div>
          </div>

          {/* Card 4 (Span 4): Offline-First Engine */}
          <div className="md:col-span-4 p-8 bg-white dark:bg-[#18221D] border border-[#E2E5DD] dark:border-[#26372F] rounded flex flex-col justify-between transition-colors">
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-10 h-10 rounded border border-[#E2E5DD] dark:border-[#26372F] bg-[#F7F7F2] dark:bg-[#111714] flex items-center justify-center text-[#173D32] dark:text-[#C8D8B9]">
                  <WifiOff className="w-5 h-5" strokeWidth={1.5} />
                </div>
                <span className="text-[11px] font-mono uppercase tracking-wider text-[#777D76] dark:text-[#8E9B93]">
                  Resilience · Capability 05
                </span>
              </div>
              <h4 className="text-lg font-semibold text-[#202521] dark:text-[#F3F6F1] mb-2">
                Offline-First Data Storage
              </h4>
              <p className="text-sm text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
                Basement warehouses and dead cellular zones never block billing. Data stores locally and syncs automatically once reconnected.
              </p>
            </div>
          </div>

          {/* Card 5 (Span 4): Push Notifications */}
          <div className="md:col-span-4 p-8 bg-white dark:bg-[#18221D] border border-[#E2E5DD] dark:border-[#26372F] rounded flex flex-col justify-between transition-colors">
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="w-10 h-10 rounded border border-[#E2E5DD] dark:border-[#26372F] bg-[#F7F7F2] dark:bg-[#111714] flex items-center justify-center text-[#173D32] dark:text-[#C8D8B9]">
                  <BellRing className="w-5 h-5" strokeWidth={1.5} />
                </div>
                <span className="text-[11px] font-mono uppercase tracking-wider text-[#777D76] dark:text-[#8E9B93]">
                  Alerts · Capability 06
                </span>
              </div>
              <h4 className="text-lg font-semibold text-[#202521] dark:text-[#F3F6F1] mb-2">
                Firebase Cloud Messaging
              </h4>
              <p className="text-sm text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
                Drivers receive immediate push dispatches for newly assigned routes and priority delivery requests.
              </p>
            </div>
          </div>

        </div>

      </div>
    </section>
  );
}

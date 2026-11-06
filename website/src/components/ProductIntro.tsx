import { APP_CONFIG } from "@/config/app.config";

export default function ProductIntro() {
  const principles = [
    {
      number: "01",
      title: "Direct Deployment Velocity",
      description:
        "By releasing directly via signed production packages, hotfixes and critical updates reach drivers and warehouse teams immediately without waiting on app store review pipelines.",
    },
    {
      number: "02",
      title: "Zero Third-Party Telemetry",
      description:
        "Every binary is compiled clean. There are no advertising SDKs, tracking pixels, or third-party analytics bloat—saving battery and cellular bandwidth on route.",
    },
    {
      number: "03",
      title: "Total Operational Independence",
      description:
        "Your organization retains complete control over distribution, version pinning, and rollouts across single handsets or company-wide Android fleets.",
    },
  ];

  return (
    <section id="overview" className="py-20 sm:py-28 border-b border-[#E2E5DD] dark:border-[#26372F] transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Eyebrow without slashes */}
        <div className="flex items-center gap-2 mb-8">
          <span className="text-xs font-mono tracking-widest text-[#777D76] dark:text-[#8E9B93] uppercase">
            System Overview
          </span>
        </div>

        {/* Two-Column Editorial Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16">
          
          {/* Left Column: Large Editorial Statement */}
          <div className="lg:col-span-5">
            <h2 className="text-3xl sm:text-4xl lg:text-5xl font-normal tracking-tight text-[#202521] dark:text-[#F3F6F1] leading-[1.15]">
              Distributing directly is not an alternative. <br />
              <span className="font-semibold text-[#173D32] dark:text-[#C8D8B9]">
                It is an operational advantage.
              </span>
            </h2>

            <div className="mt-8 p-6 bg-white dark:bg-[#18221D] border border-[#E2E5DD] dark:border-[#26372F] rounded">
              <div className="text-xs font-mono tracking-wider text-[#777D76] dark:text-[#8E9B93] uppercase mb-1">
                Release Authentication
              </div>
              <div className="text-sm font-semibold text-[#202521] dark:text-[#F3F6F1]">
                Cryptographically Signed Production Package
              </div>
              <p className="mt-2 text-xs text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
                Every release is compiled with our release keystore and published with a public SHA-256 hash. Install with confidence on any Android device from 7.0 to Android 16.
              </p>
            </div>
          </div>

          {/* Right Column: Narrative & Principles with Hairline Dividers (No slashes) */}
          <div className="lg:col-span-7 flex flex-col justify-between">
            <p className="text-lg sm:text-xl text-[#202521] dark:text-[#F3F6F1] leading-relaxed font-normal">
              {APP_CONFIG.app.name} was engineered from first principles for water can logistics: real-time stock balances across warehouses, driver GPS route dispatch, customer deposits, and instant receipt generation.
            </p>

            <div className="mt-10 divide-y divide-[#E2E5DD] dark:divide-[#26372F] border-t border-[#E2E5DD] dark:border-[#26372F]">
              {principles.map((p) => (
                <div key={p.number} className="py-6 grid grid-cols-1 sm:grid-cols-12 gap-4 items-start">
                  <div className="sm:col-span-2 text-xs font-mono font-semibold text-[#173D32] dark:text-[#C8D8B9]">
                    {p.number}
                  </div>
                  <div className="sm:col-span-10">
                    <h3 className="text-base font-semibold text-[#202521] dark:text-[#F3F6F1]">
                      {p.title}
                    </h3>
                    <p className="mt-1 text-sm text-[#777D76] dark:text-[#8E9B93] leading-relaxed">
                      {p.description}
                    </p>
                  </div>
                </div>
              ))}
            </div>

          </div>

        </div>

      </div>
    </section>
  );
}

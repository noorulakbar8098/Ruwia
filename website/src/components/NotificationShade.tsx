import { Package } from "lucide-react";
import { APP_CONFIG } from "@/config/app.config";

interface NotificationShadeProps {
  className?: string;
}

/**
 * Visual stand-in for the Android notification shade.
 *
 * People who cannot find the downloaded APK are almost always searching a file
 * manager. Android posts a completion notification that opens the system
 * package installer in a single tap, so the illustration points at it directly.
 */
export default function NotificationShade({ className = "" }: NotificationShadeProps) {
  return (
    <div
      role="img"
      aria-label={`Android notification shade showing ${APP_CONFIG.release.apkFileName} with the message download complete. Tapping the notification opens the installer.`}
      className={`rounded-2xl border border-[#2C3F34] bg-[#101815] p-3 ${className}`}
    >
      <div className="mb-2 flex items-center justify-between px-1">
        <span className="text-[10px] font-mono uppercase tracking-widest text-[#8E9B93]">
          Notification shade
        </span>
        <span className="h-1 w-6 rounded-full bg-[#8E9B93]/40" />
      </div>

      <div className="rounded-xl border border-[#C8D8B9]/40 bg-[#1B2A23] p-3 ring-1 ring-[#C8D8B9]/20">
        <div className="mb-1.5 flex items-center gap-1.5">
          <span className="h-2 w-2 rounded-full bg-[#C8D8B9]" />
          <span className="text-[10px] font-mono uppercase tracking-wider text-[#C8D8B9]">
            Chrome · just now
          </span>
        </div>

        <div className="flex items-start gap-2.5">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-[#C8D8B9]/30 bg-[#173D32]">
            <Package className="h-4 w-4 text-[#C8D8B9]" />
          </div>

          <div className="min-w-0 flex-1">
            <p className="truncate text-[11px] font-semibold text-[#F7F7F2]">
              {APP_CONFIG.release.apkFileName}
            </p>
            <p className="mt-0.5 text-[10px] text-[#9FB3A6]">
              Download complete · Tap to install
            </p>
          </div>

          <span className="mt-1 shrink-0 rounded-full bg-[#C8D8B9] px-2 py-0.5 text-[9px] font-bold uppercase tracking-wider text-[#111714]">
            Tap
          </span>
        </div>
      </div>
    </div>
  );
}

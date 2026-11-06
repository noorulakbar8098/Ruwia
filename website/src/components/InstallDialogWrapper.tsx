"use client";

import { useDownload } from "@/context/DownloadContext";
import InstallDialog from "@/components/InstallDialog";

export default function InstallDialogWrapper() {
  const { isInstallDialogOpen, closeInstallDialog } = useDownload();
  return (
    <InstallDialog
      isOpen={isInstallDialogOpen}
      onClose={closeInstallDialog}
    />
  );
}

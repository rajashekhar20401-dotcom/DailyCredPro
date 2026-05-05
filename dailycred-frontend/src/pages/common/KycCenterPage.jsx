import { useEffect, useMemo, useState } from "react";
import api from "../../api/client";
import { useAuth } from "../../auth/AuthContext";

const documentTypes = ["AADHAAR", "PAN", "SELFIE", "SIGNATURE"];

function SummaryCard({ title, value, subtitle }) {
  return (
    <div className="rounded-2xl bg-white p-5 shadow-sm">
      <p className="text-sm font-medium text-slate-500">{title}</p>
      <h3 className="mt-2 text-2xl font-bold text-slate-900">{value}</h3>
      {subtitle ? <p className="mt-1 text-xs text-slate-500">{subtitle}</p> : null}
    </div>
  );
}

function StatusPill({ text, tone = "slate" }) {
  const toneMap = {
    green: "bg-emerald-50 text-emerald-700",
    red: "bg-red-50 text-red-700",
    yellow: "bg-amber-50 text-amber-700",
    blue: "bg-blue-50 text-blue-700",
    slate: "bg-slate-100 text-slate-700",
  };

  return (
    <span className={`rounded-full px-3 py-1 text-xs font-medium ${toneMap[tone] || toneMap.slate}`}>
      {text}
    </span>
  );
}

export default function KycCenterPage() {
  const { role, userId } = useAuth();

  const ownerType = useMemo(() => {
    if (role === "BORROWER") return "BORROWER";
    if (role === "LENDER") return "LENDER";
    return null;
  }, [role]);

  const [summary, setSummary] = useState(null);
  const [documents, setDocuments] = useState([]);

  const [selectedDocumentType, setSelectedDocumentType] = useState("AADHAAR");
  const [selectedFile, setSelectedFile] = useState(null);

  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [ocrLoadingId, setOcrLoadingId] = useState(null);

  const [pageError, setPageError] = useState("");
  const [actionMessage, setActionMessage] = useState("");
  const [actionError, setActionError] = useState("");

  async function loadKycData() {
    if (!ownerType || !userId) return;

    setLoading(true);
    setPageError("");

    try {
      const [summaryResponse, docsResponse] = await Promise.all([
        api.get(`/api/kyc/${ownerType}/${userId}/summary`),
        api.get(`/api/kyc/${ownerType}/${userId}/documents`),
      ]);

      setSummary(summaryResponse.data || null);
      setDocuments(docsResponse.data || []);
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to load KYC data.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadKycData();
  }, [ownerType, userId]);

  async function handleUpload(event) {
    event.preventDefault();

    if (!selectedFile) {
      setActionError("Please choose a file before uploading.");
      setActionMessage("");
      return;
    }

    if (!ownerType || !userId) {
      setActionError("Owner type or user ID is missing.");
      setActionMessage("");
      return;
    }

    setUploading(true);
    setActionError("");
    setActionMessage("");

    try {
      const formData = new FormData();
      formData.append("file", selectedFile);

      const response = await api.post(
        `/api/kyc/${ownerType}/${userId}/${selectedDocumentType}/upload`,
        formData,
        {
          headers: {
            "Content-Type": "multipart/form-data",
          },
        }
      );

      setActionMessage(response.data?.verificationRemarks || `${selectedDocumentType} uploaded successfully.`);
      setSelectedFile(null);
      await loadKycData();
    } catch (err) {
      setActionError(err.response?.data?.message || err.message || "Upload failed.");
    } finally {
      setUploading(false);
    }
  }

  async function handleRunOcr(documentId) {
    setOcrLoadingId(documentId);
    setActionError("");
    setActionMessage("");

    try {
      const response = await api.post(`/api/kyc/documents/${documentId}/ocr`);
      setActionMessage(response.data?.verificationRemarks || "OCR processed successfully.");
      await loadKycData();
    } catch (err) {
      setActionError(err.response?.data?.message || err.message || "OCR processing failed.");
    } finally {
      setOcrLoadingId(null);
    }
  }

  if (role === "ADMIN") {
    return (
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">KYC Center</h2>
        <p className="mt-2 text-slate-600">
          KYC upload is currently available only for borrower and lender roles.
        </p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-slate-600">Loading KYC center...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">KYC & OCR Center</h2>
        <p className="mt-2 text-sm text-slate-500">
          Upload Aadhaar, PAN, selfie, and signature. Then run OCR for Aadhaar and PAN to verify extracted numbers.
        </p>

        {pageError ? (
          <div className="mt-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{pageError}</div>
        ) : null}
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          title="Completion"
          value={`${summary?.completionPercent ?? 0}%`}
          subtitle="KYC completion based on uploaded document count"
        />
        <SummaryCard
          title="Overall Status"
          value={summary?.overallStatus || "UNKNOWN"}
          subtitle="Current KYC summary status"
        />
        <SummaryCard
          title="Manual Review"
          value={summary?.manualReviewRequired ? "YES" : "NO"}
          subtitle="Triggered when OCR mismatch or extraction issue occurs"
        />
        <SummaryCard
          title="Owner Type"
          value={ownerType || "-"}
          subtitle={`Linked to logged-in user ID ${userId || "-"}`}
        />
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h3 className="mb-4 text-lg font-semibold text-slate-900">Upload Document</h3>

        <form onSubmit={handleUpload} className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Document Type</label>
            <select
              value={selectedDocumentType}
              onChange={(e) => setSelectedDocumentType(e.target.value)}
              className="w-full rounded-xl border border-slate-300 px-3 py-2"
            >
              {documentTypes.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
          </div>

          <div className="md:col-span-2">
            <label className="mb-1 block text-sm font-medium text-slate-700">Choose File</label>
            <input
              type="file"
              onChange={(e) => setSelectedFile(e.target.files?.[0] || null)}
              className="w-full rounded-xl border border-slate-300 px-3 py-2"
            />
          </div>

          <div className="md:col-span-3 flex justify-end">
            <button
              type="submit"
              disabled={uploading}
              className="rounded-xl bg-slate-900 px-6 py-3 font-medium text-white disabled:opacity-60"
            >
              {uploading ? "Uploading..." : `Upload ${selectedDocumentType}`}
            </button>
          </div>
        </form>
      </div>

      {(actionMessage || actionError) && (
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          {actionMessage ? (
            <div className="rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
              {actionMessage}
            </div>
          ) : null}

          {actionError ? (
            <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">
              {actionError}
            </div>
          ) : null}
        </div>
      )}

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-slate-900">Uploaded Documents</h3>
          <button
            onClick={loadKycData}
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
          >
            Refresh
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-slate-500">
                <th className="px-3 py-3">Type</th>
                <th className="px-3 py-3">File Name</th>
                <th className="px-3 py-3">Status</th>
                <th className="px-3 py-3">Extracted Number</th>
                <th className="px-3 py-3">Manual Review</th>
                <th className="px-3 py-3">Action</th>
              </tr>
            </thead>
            <tbody>
              {documents.length === 0 ? (
                <tr>
                  <td colSpan="6" className="px-3 py-6 text-center text-slate-500">
                    No documents uploaded yet.
                  </td>
                </tr>
              ) : (
                documents.map((doc) => (
                  <tr key={doc.documentId} className="border-b border-slate-100">
                    <td className="px-3 py-3 text-slate-700">{doc.documentType}</td>
                    <td className="px-3 py-3 text-slate-700">{doc.fileName}</td>
                    <td className="px-3 py-3 text-slate-700">
                      <StatusPill
                        text={doc.verificationStatus}
                        tone={
                          doc.verificationStatus === "VERIFIED"
                            ? "green"
                            : doc.verificationStatus === "MISMATCH"
                            ? "red"
                            : doc.verificationStatus === "MANUAL_REVIEW_REQUIRED"
                            ? "yellow"
                            : "slate"
                        }
                      />
                    </td>
                    <td className="px-3 py-3 text-slate-700">
                      {doc.extractedDocumentNumber || "-"}
                    </td>
                    <td className="px-3 py-3 text-slate-700">
                      {doc.manualReviewRequired ? "YES" : "NO"}
                    </td>
                    <td className="px-3 py-3">
                      {doc.documentType === "AADHAAR" || doc.documentType === "PAN" ? (
                        <button
                          onClick={() => handleRunOcr(doc.documentId)}
                          disabled={ocrLoadingId === doc.documentId}
                          className="rounded-xl border border-slate-300 px-4 py-2 text-xs font-medium text-slate-700"
                        >
                          {ocrLoadingId === doc.documentId ? "Processing..." : "Run OCR"}
                        </button>
                      ) : (
                        <span className="text-xs text-slate-400">No OCR</span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h3 className="mb-3 text-lg font-semibold text-slate-900">How this page works</h3>
        <div className="space-y-2 text-sm text-slate-600">
          <p>1. It detects whether the logged-in user is a borrower or lender and uses that as the KYC owner type.</p>
          <p>2. It uploads files using multipart/form-data to the backend KYC upload endpoint.</p>
          <p>3. It reloads the document list and KYC summary after upload.</p>
          <p>4. Aadhaar and PAN rows get a Run OCR button that calls the OCR endpoint for that uploaded document.</p>
        </div>
      </div>
    </div>
  );
}
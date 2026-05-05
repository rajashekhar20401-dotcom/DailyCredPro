import termsContent from "../../content/termsContent";

export default function TermsPage() {
  return (
    <div className="rounded-2xl bg-white p-6 shadow-sm">
      <h2 className="mb-4 text-2xl font-semibold text-slate-900">Terms & Conditions</h2>
      <pre className="whitespace-pre-wrap text-sm leading-7 text-slate-700">
        {termsContent}
      </pre>
    </div>
  );
}
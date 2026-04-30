import { useEffect, useMemo, useState } from "react";
import api from "../../api/client";
import { useAuth } from "../../auth/AuthContext";

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

const initialTopup = {
  amount: "",
  description: "",
};

const initialWithdraw = {
  amount: "",
  description: "",
};

export default function WalletPage() {
  const { role, userId } = useAuth();

  const ownerType = useMemo(() => {
    if (role === "BORROWER") return "BORROWER";
    if (role === "LENDER") return "LENDER";
    return null;
  }, [role]);

  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [topupForm, setTopupForm] = useState(initialTopup);
  const [withdrawForm, setWithdrawForm] = useState(initialWithdraw);

  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  const [pageError, setPageError] = useState("");
  const [actionMessage, setActionMessage] = useState("");
  const [actionError, setActionError] = useState("");

  async function loadWalletData() {
    if (!ownerType || !userId) return;

    setLoading(true);
    setPageError("");

    try {
      const [walletResponse, txResponse] = await Promise.all([
        api.get(`/api/wallets/${ownerType}/${userId}`),
        api.get(`/api/wallets/${ownerType}/${userId}/transactions`),
      ]);

      setWallet(walletResponse.data || null);
      setTransactions(txResponse.data || []);
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to load wallet data.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadWalletData();
  }, [ownerType, userId]);

  function handleTopupChange(event) {
    const { name, value } = event.target;
    setTopupForm((prev) => ({ ...prev, [name]: value }));
  }

  function handleWithdrawChange(event) {
    const { name, value } = event.target;
    setWithdrawForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleTopup(event) {
    event.preventDefault();

    if (!topupForm.amount) {
      setActionError("Top up amount is required.");
      setActionMessage("");
      return;
    }

    setActionLoading(true);
    setActionError("");
    setActionMessage("");

    try {
      const payload = {
        walletOwnerType: ownerType,
        ownerId: Number(userId),
        amount: Number(topupForm.amount),
        description: topupForm.description,
      };

      const response = await api.post("/api/wallets/topup", payload);
      setActionMessage(response.data?.message || "Wallet top up successful.");
      setTopupForm(initialTopup);
      await loadWalletData();
    } catch (err) {
      setActionError(err.response?.data?.message || err.message || "Wallet top up failed.");
    } finally {
      setActionLoading(false);
    }
  }

  async function handleWithdraw(event) {
    event.preventDefault();

    if (!withdrawForm.amount) {
      setActionError("Withdraw amount is required.");
      setActionMessage("");
      return;
    }

    setActionLoading(true);
    setActionError("");
    setActionMessage("");

    try {
      const payload = {
        walletOwnerType: ownerType,
        ownerId: Number(userId),
        amount: Number(withdrawForm.amount),
        description: withdrawForm.description,
      };

      const response = await api.post("/api/wallets/withdraw", payload);
      setActionMessage(response.data?.message || "Wallet withdraw successful.");
      setWithdrawForm(initialWithdraw);
      await loadWalletData();
    } catch (err) {
      setActionError(err.response?.data?.message || err.message || "Wallet withdraw failed.");
    } finally {
      setActionLoading(false);
    }
  }

  if (role === "ADMIN") {
    return (
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">Wallet Center</h2>
        <p className="mt-2 text-slate-600">
          Wallet self-service is currently available only for borrower and lender roles.
        </p>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <p className="text-slate-600">Loading wallet...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">Wallet Center</h2>
        <p className="mt-2 text-sm text-slate-500">
          View your wallet balance, top up money, withdraw funds, and review transaction history.
        </p>

        {pageError ? (
          <div className="mt-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">{pageError}</div>
        ) : null}
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          title="Wallet Balance"
          value={wallet?.balance ?? 0}
          subtitle="Current usable wallet amount"
        />
        <SummaryCard
          title="Wallet Status"
          value={wallet?.frozen ? "FROZEN" : "ACTIVE"}
          subtitle="Frozen wallets cannot transact"
        />
        <SummaryCard
          title="Owner Type"
          value={ownerType || "-"}
          subtitle={`Linked to user ID ${userId || "-"}`}
        />
        <SummaryCard
          title="Transaction Count"
          value={transactions.length}
          subtitle="Loaded from wallet transaction history"
        />
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          <h3 className="mb-4 text-lg font-semibold text-slate-900">Top Up Wallet</h3>

          <form onSubmit={handleTopup} className="space-y-4">
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Amount</label>
              <input
                type="number"
                name="amount"
                value={topupForm.amount}
                onChange={handleTopupChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
                placeholder="5000"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Description</label>
              <input
                name="description"
                value={topupForm.description}
                onChange={handleTopupChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Added money to wallet"
              />
            </div>

            <button
              type="submit"
              disabled={actionLoading}
              className="rounded-xl bg-slate-900 px-6 py-3 font-medium text-white disabled:opacity-60"
            >
              {actionLoading ? "Processing..." : "Top Up Wallet"}
            </button>
          </form>
        </div>

        <div className="rounded-2xl bg-white p-6 shadow-sm">
          <h3 className="mb-4 text-lg font-semibold text-slate-900">Withdraw Wallet</h3>

          <form onSubmit={handleWithdraw} className="space-y-4">
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Amount</label>
              <input
                type="number"
                name="amount"
                value={withdrawForm.amount}
                onChange={handleWithdrawChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
                placeholder="1000"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Description</label>
              <input
                name="description"
                value={withdrawForm.description}
                onChange={handleWithdrawChange}
                className="w-full rounded-xl border border-slate-300 px-3 py-2"
                placeholder="Withdraw to bank"
              />
            </div>

            <button
              type="submit"
              disabled={actionLoading}
              className="rounded-xl border border-slate-300 bg-white px-6 py-3 font-medium text-slate-700 disabled:opacity-60"
            >
              {actionLoading ? "Processing..." : "Withdraw Wallet"}
            </button>
          </form>
        </div>
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
          <h3 className="text-lg font-semibold text-slate-900">Transaction History</h3>
          <button
            onClick={loadWalletData}
            className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700"
          >
            Refresh
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="min-w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-slate-500">
                <th className="px-3 py-3">Transaction ID</th>
                <th className="px-3 py-3">Type</th>
                <th className="px-3 py-3">Amount</th>
                <th className="px-3 py-3">Description</th>
                <th className="px-3 py-3">Created At</th>
                <th className="px-3 py-3">Status</th>
              </tr>
            </thead>
            <tbody>
              {transactions.length === 0 ? (
                <tr>
                  <td colSpan="6" className="px-3 py-6 text-center text-slate-500">
                    No wallet transactions found.
                  </td>
                </tr>
              ) : (
                transactions.map((item) => (
                  <tr key={item.transactionId} className="border-b border-slate-100">
                    <td className="px-3 py-3 text-slate-700">{item.transactionId}</td>
                    <td className="px-3 py-3 text-slate-700">{item.transactionType}</td>
                    <td className="px-3 py-3 text-slate-700">{item.amount}</td>
                    <td className="px-3 py-3 text-slate-700">{item.description || "-"}</td>
                    <td className="px-3 py-3 text-slate-700">
                      {item.createdAt ? new Date(item.createdAt).toLocaleString() : "-"}
                    </td>
                    <td className="px-3 py-3 text-slate-700">
                      <StatusPill
                        text={item.status || "DONE"}
                        tone={item.status === "FAILED" ? "red" : "green"}
                      />
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
          <p>1. It determines whether the logged-in user is a borrower or lender and uses that as the wallet owner type.</p>
          <p>2. It fetches wallet details and transaction history from the backend wallet APIs.</p>
          <p>3. Top up and withdraw actions send real backend requests and then reload the wallet state.</p>
          <p>4. This is the same wallet your loan disbursement and wallet repayment flows use.</p>
        </div>
      </div>
    </div>
  );
}
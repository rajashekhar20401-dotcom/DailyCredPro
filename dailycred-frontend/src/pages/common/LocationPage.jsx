import { useMemo, useState } from "react";
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

export default function LocationPage() {
  const { role, userId } = useAuth();

  const [coords, setCoords] = useState({
    latitude: "",
    longitude: "",
    consentGiven: true,
  });

  const [radiusKm, setRadiusKm] = useState(30);
  const [nearbyLenders, setNearbyLenders] = useState([]);
  const [borrowerLookupId, setBorrowerLookupId] = useState("");
  const [lastKnownBorrowerLocation, setLastKnownBorrowerLocation] = useState(null);

  const [loadingLocation, setLoadingLocation] = useState(false);
  const [savingLocation, setSavingLocation] = useState(false);
  const [searchingNearby, setSearchingNearby] = useState(false);
  const [searchingBorrowerLocation, setSearchingBorrowerLocation] = useState(false);

  const [pageMessage, setPageMessage] = useState("");
  const [pageError, setPageError] = useState("");

  const ownerType = useMemo(() => {
    if (role === "BORROWER") return "BORROWER";
    if (role === "LENDER") return "LENDER";
    return null;
  }, [role]);

  function handleCoordChange(event) {
    const { name, value, type, checked } = event.target;
    setCoords((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  }

  function detectCurrentLocation() {
    setPageError("");
    setPageMessage("");

    if (!navigator.geolocation) {
      setPageError("Geolocation is not supported by this browser.");
      return;
    }

    setLoadingLocation(true);

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setCoords((prev) => ({
          ...prev,
          latitude: String(position.coords.latitude),
          longitude: String(position.coords.longitude),
        }));
        setPageMessage("Current location detected successfully.");
        setLoadingLocation(false);
      },
      (error) => {
        setPageError(error.message || "Failed to get current location.");
        setLoadingLocation(false);
      },
      {
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 0,
      }
    );
  }

  async function saveLocation() {
    setPageError("");
    setPageMessage("");

    if (!coords.latitude || !coords.longitude) {
      setPageError("Latitude and longitude are required.");
      return;
    }

    setSavingLocation(true);

    try {
      const payload = {
        latitude: Number(coords.latitude),
        longitude: Number(coords.longitude),
        consentGiven: coords.consentGiven,
      };

      const url =
        role === "BORROWER"
          ? `/api/location/borrowers/${userId}`
          : `/api/location/lenders/${userId}`;

      const response = await api.put(url, payload);
      setPageMessage(response.data?.message || "Location updated successfully.");
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to save location.");
    } finally {
      setSavingLocation(false);
    }
  }

  async function findNearbyLenders() {
    setPageError("");
    setPageMessage("");
    setNearbyLenders([]);

    if (role !== "BORROWER") {
      setPageError("Nearby lender search is available only for borrower accounts.");
      return;
    }

    setSearchingNearby(true);

    try {
      const response = await api.get(
        `/api/location/borrowers/${userId}/nearby-lenders`,
        {
          params: { radiusKm: Number(radiusKm) },
        }
      );

      setNearbyLenders(response.data?.data || []);
      setPageMessage("Nearby lenders fetched successfully.");
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to fetch nearby lenders.");
    } finally {
      setSearchingNearby(false);
    }
  }

  async function fetchBorrowerLastKnownLocation() {
    setPageError("");
    setPageMessage("");
    setLastKnownBorrowerLocation(null);

    if (role !== "LENDER") {
      setPageError("Borrower last known location lookup is available only for lenders.");
      return;
    }

    if (!borrowerLookupId) {
      setPageError("Please enter borrower ID.");
      return;
    }

    setSearchingBorrowerLocation(true);

    try {
      const response = await api.get(
        `/api/location/lenders/${userId}/borrowers/${borrowerLookupId}/last-known-location`
      );

      setLastKnownBorrowerLocation(response.data?.data || null);
      setPageMessage(response.data?.message || "Borrower last known location fetched successfully.");
    } catch (err) {
      setPageError(err.response?.data?.message || err.message || "Failed to fetch borrower last known location.");
    } finally {
      setSearchingBorrowerLocation(false);
    }
  }

  if (role === "ADMIN") {
    return (
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">Location Center</h2>
        <p className="mt-2 text-slate-600">
          Location features are currently available only for borrower and lender roles.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-2xl font-semibold text-slate-900">Location Center</h2>
        <p className="mt-2 text-sm text-slate-500">
          {role === "BORROWER"
            ? "Use your location to discover nearby lenders within a chosen radius."
            : "Set your service location and, when rules allow, check borrower last known location in severe default cases."}
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <SummaryCard
          title="Role"
          value={role || "-"}
          subtitle="Current logged-in account type"
        />
        <SummaryCard
          title="Latitude"
          value={coords.latitude || "-"}
          subtitle="Current form latitude"
        />
        <SummaryCard
          title="Longitude"
          value={coords.longitude || "-"}
          subtitle="Current form longitude"
        />
        <SummaryCard
          title="Consent"
          value={coords.consentGiven ? "YES" : "NO"}
          subtitle="Location consent flag sent to backend"
        />
      </div>

      {(pageMessage || pageError) && (
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          {pageMessage ? (
            <div className="rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
              {pageMessage}
            </div>
          ) : null}

          {pageError ? (
            <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600">
              {pageError}
            </div>
          ) : null}
        </div>
      )}

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h3 className="mb-4 text-lg font-semibold text-slate-900">
          {role === "BORROWER" ? "Update Borrower Location" : "Update Lender Service Location"}
        </h3>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Latitude</label>
            <input
              name="latitude"
              value={coords.latitude}
              onChange={handleCoordChange}
              className="w-full rounded-xl border border-slate-300 px-3 py-2"
              placeholder="18.6725"
            />
          </div>

          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Longitude</label>
            <input
              name="longitude"
              value={coords.longitude}
              onChange={handleCoordChange}
              className="w-full rounded-xl border border-slate-300 px-3 py-2"
              placeholder="78.0941"
            />
          </div>
        </div>

        <label className="mt-4 flex items-center gap-2 text-sm text-slate-700">
          <input
            type="checkbox"
            name="consentGiven"
            checked={coords.consentGiven}
            onChange={handleCoordChange}
          />
          I consent to storing this location for application functionality.
        </label>

        <div className="mt-5 flex flex-wrap gap-3">
          <button
            type="button"
            onClick={detectCurrentLocation}
            disabled={loadingLocation}
            className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 disabled:opacity-60"
          >
            {loadingLocation ? "Detecting..." : "Use Current Location"}
          </button>

          <button
            type="button"
            onClick={saveLocation}
            disabled={savingLocation}
            className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-60"
          >
            {savingLocation ? "Saving..." : "Save Location"}
          </button>
        </div>
      </div>

      {role === "BORROWER" ? (
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="text-lg font-semibold text-slate-900">Nearby Lenders</h3>
            <div className="flex items-center gap-3">
              <input
                type="number"
                value={radiusKm}
                onChange={(e) => setRadiusKm(e.target.value)}
                className="w-28 rounded-xl border border-slate-300 px-3 py-2 text-sm"
                placeholder="30"
              />
              <button
                type="button"
                onClick={findNearbyLenders}
                disabled={searchingNearby}
                className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-60"
              >
                {searchingNearby ? "Searching..." : "Find Nearby Lenders"}
              </button>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-full border-collapse text-left text-sm">
              <thead>
                <tr className="border-b border-slate-200 text-slate-500">
                  <th className="px-3 py-3">Lender ID</th>
                  <th className="px-3 py-3">Lender Name</th>
                  <th className="px-3 py-3">Pincode</th>
                  <th className="px-3 py-3">Latitude</th>
                  <th className="px-3 py-3">Longitude</th>
                  <th className="px-3 py-3">Distance (km)</th>
                </tr>
              </thead>
              <tbody>
                {nearbyLenders.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="px-3 py-6 text-center text-slate-500">
                      No nearby lenders loaded yet.
                    </td>
                  </tr>
                ) : (
                  nearbyLenders.map((item) => (
                    <tr key={item.lenderId} className="border-b border-slate-100">
                      <td className="px-3 py-3 text-slate-700">{item.lenderId}</td>
                      <td className="px-3 py-3 text-slate-700">{item.lenderName}</td>
                      <td className="px-3 py-3 text-slate-700">{item.pincode}</td>
                      <td className="px-3 py-3 text-slate-700">{item.latitude}</td>
                      <td className="px-3 py-3 text-slate-700">{item.longitude}</td>
                      <td className="px-3 py-3 text-slate-700">{item.distanceKm}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      ) : null}

{/*       {role === "LENDER" ? ( */}
{/*         <div className="rounded-2xl bg-white p-6 shadow-sm"> */}
{/*           <h3 className="mb-4 text-lg font-semibold text-slate-900">Borrower Last Known Location</h3> */}

{/*           <div className="grid grid-cols-1 gap-4 md:grid-cols-[1fr_auto]"> */}
{/*             <input */}
{/*               value={borrowerLookupId} */}
{/*               onChange={(e) => setBorrowerLookupId(e.target.value)} */}
{/*               className="w-full rounded-xl border border-slate-300 px-3 py-2" */}
{/*               placeholder="Enter borrower ID" */}
{/*             /> */}

{/*             <button */}
{/*               type="button" */}
{/*               onClick={fetchBorrowerLastKnownLocation} */}
{/*               disabled={searchingBorrowerLocation} */}
{/*               className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-60" */}
{/*             > */}
{/*               {searchingBorrowerLocation ? "Searching..." : "Get Last Known Location"} */}
{/*             </button> */}
{/*           </div> */}

{/*           {lastKnownBorrowerLocation ? ( */}
{/*             <div className="mt-5 rounded-2xl bg-slate-50 p-4"> */}
{/*               <div className="mb-2 flex items-center gap-3"> */}
{/*                 <StatusPill text="ACCESS GRANTED" tone="green" /> */}
{/*                 <span className="text-sm text-slate-600">{lastKnownBorrowerLocation.accessReason}</span> */}
{/*               </div> */}

{/*               <div className="grid grid-cols-1 gap-3 md:grid-cols-2"> */}
{/*                 <div> */}
{/*                   <p className="text-sm text-slate-500">Borrower ID</p> */}
{/*                   <p className="font-medium text-slate-900">{lastKnownBorrowerLocation.borrowerId}</p> */}
{/*                 </div> */}

{/*                 <div> */}
{/*                   <p className="text-sm text-slate-500">Borrower Name</p> */}
{/*                   <p className="font-medium text-slate-900">{lastKnownBorrowerLocation.borrowerName}</p> */}
{/*                 </div> */}

{/*                 <div> */}
{/*                   <p className="text-sm text-slate-500">Latitude</p> */}
{/*                   <p className="font-medium text-slate-900">{lastKnownBorrowerLocation.latitude}</p> */}
{/*                 </div> */}

{/*                 <div> */}
{/*                   <p className="text-sm text-slate-500">Longitude</p> */}
{/*                   <p className="font-medium text-slate-900">{lastKnownBorrowerLocation.longitude}</p> */}
{/*                 </div> */}

{/*                 <div className="md:col-span-2"> */}
{/*                   <p className="text-sm text-slate-500">Last Updated</p> */}
{/*                   <p className="font-medium text-slate-900"> */}
{/*                     {lastKnownBorrowerLocation.lastUpdatedAt */}
{/*                       ? new Date(lastKnownBorrowerLocation.lastUpdatedAt).toLocaleString() */}
{/*                       : "-"} */}
{/*                   </p> */}
{/*                 </div> */}
{/*               </div> */}
{/*             </div> */}
{/*           ) : null} */}
{/*         </div> */}
{/*       ) : null} */}

      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h3 className="mb-3 text-lg font-semibold text-slate-900">How this page works</h3>
        <div className="space-y-2 text-sm text-slate-600">
          <p>1. The browser geolocation API can detect the user’s current latitude and longitude.</p>
          <p>2. Borrowers use location to search nearby lenders within a chosen radius.</p>
          <p>3. Lenders treat saved location as service/business location, not something that must be updated every login.</p>
          <p>4. Last known borrower location is only fetched through lender lookup when backend default rules allow it.</p>
        </div>
      </div>
    </div>
  );
}
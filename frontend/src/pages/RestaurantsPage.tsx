import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { MapPin } from "lucide-react";
import { catalogApi } from "../api";
import { Empty, Loading } from "../components/ui";
import type { Branch } from "../types";
import { clock, errorMessage } from "../utils";

export function RestaurantsPage() {
  const [branches, setBranches] = useState<Branch[] | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    catalogApi.branches().then(setBranches).catch((reason) => setError(errorMessage(reason, "Branches could not be loaded.")));
  }, []);

  if (error) return <div className="mx-auto max-w-6xl px-4 py-10"><p className="text-red-800">{error}</p></div>;
  if (!branches) return <Loading label="Finding kitchens" />;

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <h1 className="font-display text-4xl">Restaurants</h1>
      <p className="mt-2 max-w-2xl text-bark">Choose one Hearthline. Your cart stays with that branch until you clear it or switch.</p>
      {branches.length === 0 ? <div className="mt-8"><Empty title="No branches open" body="Check back when a kitchen is active." /></div> : (
        <div className="mt-8 grid gap-5 md:grid-cols-3">
          {branches.map((branch) => (
            <article key={branch.id} className="card flex flex-col p-5">
              <p className="text-xs uppercase tracking-wider text-bark">{branch.city}</p>
              <h2 className="mt-1 font-display text-2xl">{branch.name.replace("Hearthline ", "")}</h2>
              <p className="mt-3 flex gap-2 text-sm text-bark"><MapPin size={16} className="mt-0.5 shrink-0" />{branch.addressLine}, {branch.city} {branch.postalCode}</p>
              <p className="mt-2 text-sm text-bark">{clock(branch.opensAt)} – {clock(branch.closesAt)} · {branch.phone}</p>
              <p className="mt-3 text-sm font-medium">{branch.acceptingOrders ? "Accepting orders" : "Not accepting orders right now"}</p>
              <Link to={`/branches/${branch.id}`} className="btn-primary mt-5">{branch.acceptingOrders ? "View menu" : "View menu anyway"}</Link>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}

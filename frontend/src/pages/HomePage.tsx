import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { catalogApi } from "../api";
import { DietMark, FoodImage } from "../components/ui";
import type { Category, MenuItem } from "../types";
import { errorMessage, inr } from "../utils";

const steps = [
  ["Choose a branch", "Each order belongs to one Hearthline kitchen."],
  ["Build the order", "Prices on the menu are the prices you pay."],
  ["Demo checkout", "The payment step is clearly marked DEMO."],
  ["Watch the kitchen", "Status moves from placed to ready in real time."]
];

export function HomePage() {
  const [featured, setFeatured] = useState<MenuItem[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([catalogApi.featured(), catalogApi.categories()])
      .then(([items, cats]) => {
        setFeatured(items);
        setCategories(cats);
      })
      .catch((reason) => setError(errorMessage(reason, "The menu could not be loaded.")));
  }, []);

  return (
    <div>
      <section className="relative mx-auto mt-4 max-w-6xl overflow-hidden rounded-[2rem] bg-ink text-cream">
        <img
          src="https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=1800&q=80"
          alt="A set table in a warm dining room"
          className="absolute inset-0 h-full w-full object-cover opacity-50"
        />
        <div className="relative max-w-xl px-6 py-16 md:px-12 md:py-24">
          <p className="text-sm uppercase tracking-[0.2em] text-cream/80">Direct from the kitchen</p>
          <h1 className="mt-3 font-display text-5xl leading-tight md:text-6xl">Order the branch nearest your table.</h1>
          <p className="mt-4 text-cream/85">Hearthline cooks for one location at a time. Pick a kitchen, send the order, and follow it until it is ready.</p>
          <Link to="/restaurants" className="btn-primary mt-8">Order now</Link>
        </div>
      </section>

      <section className="mx-auto mt-16 max-w-6xl px-4">
        <div className="flex items-end justify-between">
          <h2 className="font-display text-3xl">Featured plates</h2>
          <Link to="/restaurants" className="text-sm text-ember-dark">Choose a branch</Link>
        </div>
        {error && <p className="mt-4 text-sm text-red-800">{error}</p>}
        <div className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {featured.map((item) => (
            <article key={item.id} className="card overflow-hidden">
              <div className="h-48"><FoodImage src={item.imageUrl} alt={item.name} /></div>
              <div className="p-5">
                <DietMark vegetarian={item.vegetarian} />
                <h3 className="mt-2 font-display text-2xl">{item.name}</h3>
                <p className="mt-1 text-sm text-bark">{item.description}</p>
                <p className="mt-3 font-medium">{inr(item.price)}</p>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="mx-auto mt-16 max-w-6xl px-4">
        <h2 className="font-display text-3xl">Popular categories</h2>
        <div className="mt-6 grid grid-cols-2 gap-3 md:grid-cols-3">
          {categories.map((category) => (
            <div key={category.id} className="rounded-3xl border border-line bg-paper px-4 py-5">
              <p className="font-display text-xl">{category.name}</p>
              <p className="mt-1 text-sm text-bark">{category.description}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="mx-auto mt-16 grid max-w-6xl gap-8 px-4 md:grid-cols-2">
        <div>
          <h2 className="font-display text-3xl">How ordering works</h2>
          <ol className="mt-6 space-y-4">
            {steps.map(([title, body], index) => (
              <li key={title} className="flex gap-4">
                <span className="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-ink text-sm text-cream">{index + 1}</span>
                <div>
                  <p className="font-medium">{title}</p>
                  <p className="text-sm text-bark">{body}</p>
                </div>
              </li>
            ))}
          </ol>
        </div>
        <div className="card p-6">
          <h2 className="font-display text-3xl">Why Hearthline</h2>
          <ul className="mt-4 space-y-3 text-sm text-bark">
            <li>One cart, one branch. Items from different kitchens never share an order.</li>
            <li>The kitchen sees a ticket only after the order is saved and the demo payment succeeds.</li>
            <li>Vegetarian dishes are marked, and a paused branch cannot take a new order.</li>
          </ul>
        </div>
      </section>
    </div>
  );
}

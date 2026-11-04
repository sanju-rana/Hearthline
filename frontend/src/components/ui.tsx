import { Leaf } from "lucide-react";
import type { OrderStatus, PaymentStatus } from "../types";
import { STATUS_LABEL } from "../utils";

export function DietMark({ vegetarian }: { vegetarian: boolean }) {
  return (
    <span className={`inline-flex items-center gap-1 text-xs font-medium ${vegetarian ? "text-sage" : "text-ember-dark"}`}>
      <span className={`h-2.5 w-2.5 rounded-sm border ${vegetarian ? "border-sage" : "border-ember"}`} />
      {vegetarian ? <Leaf size={12} /> : null}
      {vegetarian ? "Vegetarian" : "Non-vegetarian"}
    </span>
  );
}

export function StatusPill({ status }: { status: OrderStatus | PaymentStatus | string }) {
  const tone =
    status === "READY" || status === "SUCCESS" || status === "COMPLETED"
      ? "bg-sage/10 text-sage"
      : status === "REJECTED" || status === "CANCELLED" || status === "FAILED"
        ? "bg-red-100 text-red-800"
        : status === "PREPARING" || status === "PENDING"
          ? "bg-amber-100 text-amber-900"
          : "bg-ember/10 text-ember-dark";
  return <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${tone}`}>{STATUS_LABEL[status] ?? status}</span>;
}

export function Notice({ children }: { children: string }) {
  return <p className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-800">{children}</p>;
}

export function Loading({ label = "Loading" }: { label?: string }) {
  return <p className="py-16 text-center text-bark">{label}...</p>;
}

export function Empty({ title, body }: { title: string; body: string }) {
  return (
    <div className="card px-6 py-14 text-center">
      <h2 className="font-display text-2xl">{title}</h2>
      <p className="mx-auto mt-2 max-w-md text-sm text-bark">{body}</p>
    </div>
  );
}

export function FoodImage({ src, alt }: { src: string | null; alt: string }) {
  if (!src) {
    return <div className="h-full w-full bg-ember/10" />;
  }
  return <img src={src} alt={alt} className="h-full w-full object-cover" />;
}

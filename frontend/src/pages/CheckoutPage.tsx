import { useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate } from "react-router-dom";
import { z } from "zod";
import { orderApi } from "../api";
import { Notice } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";
import { errorMessage, inr } from "../utils";

const schema = z.object({
  customerName: z.string().min(1).max(80),
  phone: z.string().regex(/^[0-9]{10}$/, "Enter a 10-digit phone number."),
  orderType: z.enum(["PICKUP", "DELIVERY"]),
  deliveryAddress: z.string().max(300).optional(),
  specialInstructions: z.string().max(300).optional()
}).superRefine((value, context) => {
  if (value.orderType === "DELIVERY" && !value.deliveryAddress?.trim()) {
    context.addIssue({ code: "custom", path: ["deliveryAddress"], message: "A delivery address is required." });
  }
});

type FormValues = z.infer<typeof schema>;

export function CheckoutPage() {
  const { user } = useAuth();
  const { cart, refresh } = useCart();
  const navigate = useNavigate();
  const key = useRef(crypto.randomUUID());
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      customerName: user?.fullName ?? "",
      phone: user?.phone ?? "",
      orderType: "PICKUP",
      deliveryAddress: "",
      specialInstructions: ""
    }
  });
  const orderType = form.watch("orderType");

  async function submit(values: FormValues, outcome: "SUCCESS" | "FAILURE" | "PENDING") {
    if (submitting) return;
    setSubmitting(true);
    setError("");
    try {
      const order = await orderApi.place({ ...values, paymentOutcome: outcome }, key.current);
      await refresh();
      navigate(`/orders/${order.id}/confirmation`);
    } catch (reason) {
      key.current = crypto.randomUUID();
      setError(errorMessage(reason, "Your order could not be placed. Please try again."));
    } finally {
      setSubmitting(false);
    }
  }

  if (!cart || cart.items.length === 0) {
    return <div className="mx-auto max-w-xl px-4 py-10"><Notice>Your cart is empty.</Notice></div>;
  }

  const total = orderType === "DELIVERY" ? cart.deliveryTotal : cart.pickupTotal;

  return (
    <form className="mx-auto grid max-w-5xl gap-8 px-4 py-10 lg:grid-cols-[1.1fr_0.9fr]" onSubmit={form.handleSubmit((values) => submit(values, "SUCCESS"))}>
      <div className="space-y-4">
        <h1 className="font-display text-4xl">Checkout</h1>
        <p className="text-sm text-bark">Payment on this page is a labeled demo. No card number is collected.</p>
        {error && <Notice>{error}</Notice>}
        <label className="block"><span className="label">Name</span><input className="field" {...form.register("customerName")} /></label>
        <label className="block"><span className="label">Phone</span><input className="field" {...form.register("phone")} /></label>
        <fieldset className="space-y-2">
          <legend className="label">Order type</legend>
          <label className="mr-4 text-sm"><input type="radio" value="PICKUP" {...form.register("orderType")} /> Pickup</label>
          <label className="text-sm"><input type="radio" value="DELIVERY" {...form.register("orderType")} /> Delivery</label>
        </fieldset>
        {orderType === "DELIVERY" && (
          <label className="block"><span className="label">Delivery address</span><textarea className="field" rows={3} {...form.register("deliveryAddress")} /></label>
        )}
        <label className="block"><span className="label">Note for the kitchen</span><textarea className="field" rows={2} {...form.register("specialInstructions")} /></label>
        {form.formState.errors.deliveryAddress && <Notice>{form.formState.errors.deliveryAddress.message ?? "Check the address."}</Notice>}
        {form.formState.errors.phone && <Notice>{form.formState.errors.phone.message ?? "Check the phone number."}</Notice>}
      </div>
      <aside className="card h-fit p-5">
        <h2 className="font-display text-2xl">{cart.branch?.name}</h2>
        <ul className="mt-4 space-y-2 text-sm">
          {cart.items.map((item) => <li key={item.id} className="flex justify-between"><span>{item.quantity} × {item.name}</span><span>{inr(item.lineTotal)}</span></li>)}
        </ul>
        <p className="mt-4 flex justify-between text-sm"><span>Subtotal</span><span>{inr(cart.subtotal)}</span></p>
        <p className="mt-2 flex justify-between text-sm"><span>Tax</span><span>{inr(cart.taxAmount)}</span></p>
        <p className="mt-2 flex justify-between text-sm"><span>Delivery</span><span>{inr(orderType === "DELIVERY" ? cart.deliveryFeeIfDelivery : 0)}</span></p>
        <p className="mt-3 flex justify-between font-medium"><span>Payable</span><span>{inr(total)}</span></p>
        <button className="btn-primary mt-5 w-full" disabled={submitting} type="submit">{submitting ? "Placing order..." : "Pay with demo checkout"}</button>
        <button className="btn-secondary mt-3 w-full" type="button" disabled={submitting} onClick={form.handleSubmit((values) => submit(values, "FAILURE"))}>Simulate declined payment</button>
      </aside>
    </form>
  );
}

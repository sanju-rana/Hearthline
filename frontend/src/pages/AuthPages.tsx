import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, useNavigate } from "react-router-dom";
import { z } from "zod";
import { useState } from "react";
import { Notice } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { errorMessage } from "../utils";

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1)
});

const registerSchema = z.object({
  fullName: z.string().min(1).max(80),
  email: z.string().email(),
  phone: z.string().regex(/^[0-9]{10}$/, "Enter a 10-digit phone number."),
  password: z.string().min(8).max(72).regex(/[A-Za-z]/).regex(/[0-9]/)
});

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState("");
  const form = useForm<z.infer<typeof loginSchema>>({ resolver: zodResolver(loginSchema) });

  return (
    <form className="mx-auto max-w-md px-4 py-12" onSubmit={form.handleSubmit(async (values) => {
      setError("");
      try {
        const user = await login(values.email, values.password);
        navigate(user.role === "KITCHEN_STAFF" ? "/kitchen" : user.role === "RESTAURANT_ADMIN" ? "/admin" : "/restaurants");
      } catch (reason) {
        setError(errorMessage(reason, "The email or password is incorrect."));
      }
    })}>
      <h1 className="font-display text-4xl">Login</h1>
      {error && <div className="mt-4"><Notice>{error}</Notice></div>}
      <label className="mt-5 block"><span className="label">Email</span><input className="field" {...form.register("email")} /></label>
      <label className="mt-4 block"><span className="label">Password</span><input className="field" type="password" {...form.register("password")} /></label>
      <button className="btn-primary mt-6 w-full" type="submit">Sign in</button>
      <p className="mt-4 text-sm text-bark">New here? <Link to="/register" className="text-ember-dark">Create a customer account</Link></p>
    </form>
  );
}

export function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState("");
  const form = useForm<z.infer<typeof registerSchema>>({ resolver: zodResolver(registerSchema) });

  return (
    <form className="mx-auto max-w-md px-4 py-12" onSubmit={form.handleSubmit(async (values) => {
      setError("");
      try {
        await register(values);
        navigate("/restaurants");
      } catch (reason) {
        setError(errorMessage(reason, "The account could not be created."));
      }
    })}>
      <h1 className="font-display text-4xl">Create account</h1>
      <p className="mt-2 text-sm text-bark">This form creates a customer. Kitchen and admin accounts are issued by the restaurant.</p>
      {error && <div className="mt-4"><Notice>{error}</Notice></div>}
      <label className="mt-5 block"><span className="label">Name</span><input className="field" {...form.register("fullName")} /></label>
      <label className="mt-4 block"><span className="label">Email</span><input className="field" {...form.register("email")} /></label>
      <label className="mt-4 block"><span className="label">Phone</span><input className="field" {...form.register("phone")} /></label>
      <label className="mt-4 block"><span className="label">Password</span><input className="field" type="password" {...form.register("password")} /></label>
      <button className="btn-primary mt-6 w-full" type="submit">Create account</button>
    </form>
  );
}

export interface Branding {
  slug: string;
  name: string;
  shortName: string;
  greeting: string;
  tagline: string | null;
  primaryColor: string;
  accentColor: string;
  softColor: string;
  logoUrl: string | null;
  currency: string;
  locale: string;
  timeZone: string;
  terms: Record<string, string>;
  enabledModules: string[];
  whatsappNumber: string | null;
  /** Ways donations are received, for this temple's country (UPI in India, Zelle in the US) */
  paymentModes: { value: PaymentMode; label: string }[];
}

export interface Me {
  email: string;
  displayName: string;
  roles: string[];
  /** Admin-app areas this person's roles open, e.g. "donations", "events", "users" */
  modules: string[];
  mustChangePassword: boolean;
}

export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface FamilyMember {
  name: string | null;
  birthDate: string | null;
}

export interface Family {
  spouse: FamilyMember | null;
  children: FamilyMember[];
}

export interface Contact {
  id: string;
  firstName: string;
  lastName: string | null;
  fullName: string;
  email: string | null;
  phone: string | null;
  city: string | null;
  gotra: string | null;
  nakshatra: string | null;
  birthDate: string | null;
  family: Family | null;
  initiatedName: string | null;
  spiritualMaster: string | null;
  devoteeStatus: 'VISITOR' | 'PRACTICING' | 'FIRST_INITIATED' | 'SECOND_INITIATED' | null;
  /** Extra details, e.g. initiatedName, spiritualMaster and source from the website form */
  customFields?: Record<string, unknown>;
  consentEmail: boolean;
  consentWhatsapp: boolean;
}

export type PaymentMode = 'CASH' | 'CHEQUE' | 'UPI' | 'ZELLE' | 'CARD' | 'BANK_TRANSFER' | 'ONLINE' | 'IN_KIND';

export type MatchedBy = 'EMAIL' | 'PHONE' | 'NAME' | 'CREATED';

export interface Donation {
  id: string;
  receiptNo: string;
  contactId: string | null;
  donorName: string;
  donorLink: { contactId: string; name: string; matchedBy: MatchedBy } | null;
  fundId: string;
  fundName: string;
  amount: number;
  currency: string;
  paymentMode: PaymentMode;
  reference: string | null;
  donatedAt: string;
  status: 'RECORDED' | 'VOIDED';
  voidReason: string | null;
  notes: string | null;
}

export interface Fund {
  id: string;
  name: string;
  taxDeductible: boolean;
}

export interface DashboardSummary {
  currency: string;
  todayDonations: number;
  todayCount: number;
  monthDonations: number;
  monthExpenses: number;
  monthNet: number;
  monthByFund: { fund: string; amount: number }[];
}

export interface DonationTick {
  receiptNo: string;
  amount: number;
  fund: string | null;
  donor: string | null;
  mode: string;
  voided: boolean;
}

export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED';

export interface TempleEvent {
  id: string;
  slug: string;
  title: string;
  summary: string | null;
  description: string | null;
  startsAt: string;
  endsAt: string | null;
  location: string | null;
  imageUrl: string | null;
  registrationUrl: string | null;
  status: EventStatus;
  publicUrl: string;
}

export interface DonorTotal {
  contactId: string;
  name: string | null;
  email: string | null;
  phone: string | null;
  gifts: number;
  total: number;
  thisYear: number;
  firstGift: string | null;
  lastGift: string | null;
}

export interface DonorPage {
  content: DonorTotal[];
  totalDonors: number;
  grandTotal: number;
}

export interface RoleInfo {
  role: string;
  label: string;
  description: string;
  modules: string[];
}

export interface UserAccount {
  id: string;
  email: string;
  displayName: string;
  roles: string[];
  active: boolean;
  mustChangePassword: boolean;
  lastLoginAt: string | null;
  createdAt: string;
}

export interface PasswordIssued {
  user: UserAccount;
  emailSent: boolean;
  temporaryPassword: string | null;
}

export type ExpensePurpose =
  | 'BUILDING_MAINTENANCE' | 'CONSTRUCTION' | 'DEITY_WORSHIP' | 'GROCERY' | 'MONK_DONATION' | 'BOOKS' | 'OTHER';

export interface Expense {
  id: string;
  contactId: string | null;
  name: string;
  spentOn: string;
  amount: number;
  purpose: ExpensePurpose;
  purposeLabel: string;
  notes: string | null;
}

export interface ExpenseSummary {
  thisMonth: number;
  thisYear: number;
  thisMonthByPurpose: { purpose: ExpensePurpose; label: string; amount: number }[];
}

export interface PayeeTotal {
  contactId: string;
  name: string | null;
  email: string | null;
  phone: string | null;
  expenses: number;
  total: number;
  thisYear: number;
  firstExpense: string | null;
  lastExpense: string | null;
}

export interface PayeePage {
  content: PayeeTotal[];
  totalPeople: number;
  grandTotal: number;
}

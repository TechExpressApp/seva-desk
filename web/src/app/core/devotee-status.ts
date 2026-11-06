/** Where a contact is on the path of bhakti (matches DevoteeStatus.java). Optional on every contact. */
export type DevoteeStatus = 'VISITOR' | 'PRACTICING' | 'FIRST_INITIATED' | 'SECOND_INITIATED';

export const DEVOTEE_STATUSES: { value: DevoteeStatus; label: string }[] = [
  { value: 'VISITOR', label: 'Visitor' },
  { value: 'PRACTICING', label: 'Practicing Devotee' },
  { value: 'FIRST_INITIATED', label: 'First Initiated' },
  { value: 'SECOND_INITIATED', label: 'Second Initiated' },
];

export function devoteeStatusLabel(value: string | null | undefined): string {
  return DEVOTEE_STATUSES.find((s) => s.value === value)?.label ?? '';
}

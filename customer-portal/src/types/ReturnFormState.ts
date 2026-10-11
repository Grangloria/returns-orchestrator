import type { ReturnItem } from './ReturnItem';

export interface ReturnFormState {
    step: number;
    orderId: string;
    customerEmail: string;
    zipCode: string;
    selectedItem?: ReturnItem;
    evidenceKeys: string[];
}
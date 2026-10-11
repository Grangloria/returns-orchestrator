import React, { createContext, useContext, useState } from 'react';
import type { ReturnFormState } from '../types';

interface ReturnWizardContextType {
    state: ReturnFormState;
    nextStep: () => void;
    prevStep: () => void;
    updateState: (fields: Partial<ReturnFormState>) => void;
    addEvidenceKey: (key: string) => void;
    resetWizard: () => void;
}

const initialState: ReturnFormState = {
    step: 1,
    orderId: '',
    customerEmail: '',
    zipCode: '',
    evidenceKeys: [],
};

const ReturnWizardContext = createContext<ReturnWizardContextType | undefined>(undefined);

export const ReturnWizardProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [state, setState] = useState<ReturnFormState>(initialState);

    const nextStep = () => setState((prev) => ({ ...prev, step: Math.min(prev.step + 1, 4) }));
    const prevStep = () => setState((prev) => ({ ...prev, step: Math.max(prev.step - 1, 1) }));
    const updateState = (fields: Partial<ReturnFormState>) =>
        setState((prev) => ({ ...prev, ...fields }));
    const addEvidenceKey = (key: string) =>
        setState((prev) => ({ ...prev, evidenceKeys: [...prev.evidenceKeys, key] }));
    const resetWizard = () => setState(initialState);

    return (
        <ReturnWizardContext.Provider
            value={{ state, nextStep, prevStep, updateState, addEvidenceKey, resetWizard }}
        >
            {children}
        </ReturnWizardContext.Provider>
    );
};

export const useReturnWizard = () => {
    const context = useContext(ReturnWizardContext);
    if (!context) {
        throw new Error('useReturnWizard must be used within a ReturnWizardProvider');
    }
    return context;
};
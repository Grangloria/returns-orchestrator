import React, { useState } from 'react';
import { useReturnWizard } from '../context/ReturnWizardContext';
import { submitReturnRequest } from '../api/returnApi';
import type { ReturnSubmissionResponse } from '../types';
import { ArrowLeft, CheckCircle2, Loader2, Send } from 'lucide-react';

export const Step4Summary: React.FC = () => {
    const { state, prevStep, resetWizard } = useReturnWizard();
    const [submitting, setSubmitting] = useState(false);
    const [result, setResult] = useState<ReturnSubmissionResponse | null>(null);
    const [error, setError] = useState('');

    const handleSubmit = async () => {
        if (!state.selectedItem) return;

        setSubmitting(true);
        setError('');

        try {
            const response = await submitReturnRequest({
                orderId: state.orderId,
                sku: state.selectedItem.sku,
                item: state.selectedItem.name,
                customerEmail: state.customerEmail,
                quantity: state.selectedItem.quantity,
                zipCode: state.zipCode,
                reason: state.selectedItem.reason,
                evidenceKeys: state.evidenceKeys,
            });

            setResult(response);
        } catch (err: any) {
            setError(err?.response?.data?.message || 'Failed to submit return request.');
        } finally {
            setSubmitting(false);
        }
    };

    if (result) {
        return (
            <div className="max-w-md mx-auto bg-white p-6 rounded-xl shadow-md border border-gray-100 text-center">
                <CheckCircle2 className="w-12 h-12 text-green-500 mx-auto mb-3" />
                <h2 className="text-xl font-bold text-gray-800 mb-1">Return Request Submitted</h2>
                <p className="text-sm text-gray-500 mb-4">Status: <span className="font-semibold text-green-600">{result.status}</span></p>
                <div className="bg-gray-50 p-3 rounded-lg text-xs text-gray-600 mb-6 font-mono">
                    Order ID: {result.orderId}
                </div>
                <button
                    onClick={resetWizard}
                    className="bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium py-2 px-6 rounded-lg"
                >
                    Start Another Return
                </button>
            </div>
        );
    }

    return (
        <div className="max-w-md mx-auto bg-white p-6 rounded-xl shadow-md border border-gray-100">
            <h2 className="text-xl font-bold text-gray-800 mb-4">Review & Submit</h2>

            {error && <div className="mb-4 text-xs text-red-600 bg-red-50 p-3 rounded-lg border border-red-200">{error}</div>}

            <div className="space-y-3 text-sm text-gray-600 bg-gray-50 p-4 rounded-lg mb-6">
                <div><span className="font-semibold text-gray-800">Order ID:</span> {state.orderId}</div>
                <div><span className="font-semibold text-gray-800">Email:</span> {state.customerEmail}</div>
                <div><span className="font-semibold text-gray-800">Item:</span> {state.selectedItem?.name} ({state.selectedItem?.sku})</div>
                <div><span className="font-semibold text-gray-800">Reason:</span> {state.selectedItem?.reason}</div>
                <div><span className="font-semibold text-gray-800">Attached Photos:</span> {state.evidenceKeys.length}</div>
            </div>

            <div className="flex justify-between gap-3">
                <button
                    onClick={prevStep}
                    disabled={submitting}
                    className="flex-1 border border-gray-300 hover:bg-gray-50 text-gray-700 py-2 rounded-lg text-sm font-medium flex items-center justify-center gap-1"
                >
                    <ArrowLeft className="w-4 h-4" /> Back
                </button>
                <button
                    onClick={handleSubmit}
                    disabled={submitting}
                    className="flex-1 bg-green-600 hover:bg-green-700 text-white py-2 rounded-lg text-sm font-medium flex items-center justify-center gap-1"
                >
                    {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <><Send className="w-4 h-4" /> Submit Return</>}
                </button>
            </div>
        </div>
    );
};
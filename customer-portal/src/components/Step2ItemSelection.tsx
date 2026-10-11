import React, { useState } from 'react';
import { useReturnWizard } from '../context/ReturnWizardContext';
import { ArrowLeft, ArrowRight, CheckCircle } from 'lucide-react';

export const Step2ItemSelection: React.FC = () => {
    const { state, updateState, nextStep, prevStep } = useReturnWizard();
    const [reason, setReason] = useState(state.selectedItem?.reason || 'Defective');

    // Simulated items associated with the order ID
    const mockItem = {
        sku: 'SKU-MOUSE-01',
        name: 'Wireless Ergonomic Mouse',
        quantity: 1,
    };

    const handleContinue = () => {
        updateState({
            selectedItem: { ...mockItem, reason },
        });
        nextStep();
    };

    return (
        <div className="max-w-md mx-auto bg-white p-6 rounded-xl shadow-md border border-gray-100">
            <h2 className="text-xl font-bold text-gray-800 mb-2">Select Item to Return</h2>
            <p className="text-sm text-gray-500 mb-6">Order ID: <span className="font-semibold text-gray-700">{state.orderId}</span></p>

            <div className="p-4 border border-blue-200 bg-blue-50/50 rounded-lg mb-6 flex items-start gap-3">
                <CheckCircle className="w-5 h-5 text-blue-600 mt-0.5" />
                <div>
                    <p className="font-semibold text-gray-800 text-sm">{mockItem.name}</p>
                    <p className="text-xs text-gray-500">SKU: {mockItem.sku} • Qty: {mockItem.quantity}</p>
                </div>
            </div>

            <div className="mb-6">
                <label className="block text-xs font-semibold text-gray-600 uppercase mb-2">Reason for Return</label>
                <select
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    className="w-full py-2 px-3 text-sm border rounded-lg bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                    <option value="Defective">Defective / Item Not Working</option>
                    <option value="Wrong Item">Wrong Item Received</option>
                    <option value="Damaged">Damaged During Transit</option>
                    <option value="Not as Described">Item Not as Described</option>
                </select>
            </div>

            <div className="flex justify-between gap-3">
                <button
                    onClick={prevStep}
                    className="flex-1 border border-gray-300 hover:bg-gray-50 text-gray-700 py-2 rounded-lg text-sm font-medium flex items-center justify-center gap-1"
                >
                    <ArrowLeft className="w-4 h-4" /> Back
                </button>
                <button
                    onClick={handleContinue}
                    className="flex-1 bg-blue-600 hover:bg-blue-700 text-white py-2 rounded-lg text-sm font-medium flex items-center justify-center gap-1"
                >
                    Next <ArrowRight className="w-4 h-4" />
                </button>
            </div>
        </div>
    );
};
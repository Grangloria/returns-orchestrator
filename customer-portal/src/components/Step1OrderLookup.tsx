import React, { useState } from 'react';
import { useReturnWizard } from '../context/ReturnWizardContext';
import { Search, Package, Mail, MapPin } from 'lucide-react';

export const Step1OrderLookup: React.FC = () => {
    const { state, updateState, nextStep } = useReturnWizard();
    const [orderId, setOrderId] = useState(state.orderId || '');
    const [email, setEmail] = useState(state.customerEmail || '');
    const [zipCode, setZipCode] = useState(state.zipCode || '');
    const [error, setError] = useState('');

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!orderId || !email || !zipCode) {
            setError('Please fill in all fields to locate your order.');
            return;
        }
        setError('');
        updateState({ orderId, customerEmail: email, zipCode });
        nextStep();
    };

    return (
        <div className="max-w-md mx-auto bg-white p-6 rounded-xl shadow-md border border-gray-100">
            <h2 className="text-xl font-bold text-gray-800 mb-2">Find Your Order</h2>
            <p className="text-sm text-gray-500 mb-6">Enter your details to initiate a return request.</p>

            {error && <div className="mb-4 text-xs text-red-600 bg-red-50 p-3 rounded-lg border border-red-200">{error}</div>}

            <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                    <label className="block text-xs font-semibold text-gray-600 uppercase mb-1">Order ID</label>
                    <div className="relative">
                        <Package className="w-4 h-4 text-gray-400 absolute left-3 top-3" />
                        <input
                            type="text"
                            placeholder="e.g. ORD-4108-99"
                            value={orderId}
                            onChange={(e) => setOrderId(e.target.value)}
                            className="w-full pl-9 pr-3 py-2 text-sm border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                </div>

                <div>
                    <label className="block text-xs font-semibold text-gray-600 uppercase mb-1">Customer Email</label>
                    <div className="relative">
                        <Mail className="w-4 h-4 text-gray-400 absolute left-3 top-3" />
                        <input
                            type="email"
                            placeholder="james.hayes@grangloria.com"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className="w-full pl-9 pr-3 py-2 text-sm border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                </div>

                <div>
                    <label className="block text-xs font-semibold text-gray-600 uppercase mb-1">ZIP Code</label>
                    <div className="relative">
                        <MapPin className="w-4 h-4 text-gray-400 absolute left-3 top-3" />
                        <input
                            type="text"
                            placeholder="90210"
                            value={zipCode}
                            onChange={(e) => setZipCode(e.target.value)}
                            className="w-full pl-9 pr-3 py-2 text-sm border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                </div>

                <button
                    type="submit"
                    className="w-full mt-2 bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 rounded-lg transition-colors flex items-center justify-center gap-2"
                >
                    <Search className="w-4 h-4" /> Find Order
                </button>
            </form>
        </div>
    );
};
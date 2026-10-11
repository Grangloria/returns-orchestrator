import React, { useState } from 'react';
import { useReturnWizard } from '../context/ReturnWizardContext';
import { getPresignedUrl, uploadImageToS3 } from '../api/returnApi';
import { Upload, Check, Loader2, ArrowLeft, ArrowRight, Image as ImageIcon } from 'lucide-react';

export const Step3EvidenceUpload: React.FC = () => {
    const { state, addEvidenceKey, nextStep, prevStep } = useReturnWizard();
    const [uploading, setUploading] = useState(false);
    const [error, setError] = useState('');

    const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        setUploading(true);
        setError('');

        try {
            // 1. Request presigned URL from returns-service via Gateway
            const { uploadUrl, s3Key } = await getPresignedUrl(state.orderId, file);

            // 2. Upload binary file directly to LocalStack S3
            await uploadImageToS3(uploadUrl, file);

            // 3. Save key in wizard context
            addEvidenceKey(s3Key);
        } catch (err: any) {
            setError(err?.response?.data?.message || 'Failed to upload photo to S3 storage.');
        } finally {
            setUploading(false);
        }
    };

    return (
        <div className="max-w-md mx-auto bg-white p-6 rounded-xl shadow-md border border-gray-100">
            <h2 className="text-xl font-bold text-gray-800 mb-2">Upload Evidence Photo</h2>
            <p className="text-sm text-gray-500 mb-6">Attach photos of the item defect or damage.</p>

            {error && <div className="mb-4 text-xs text-red-600 bg-red-50 p-3 rounded-lg border border-red-200">{error}</div>}

            <div className="border-2 border-dashed border-gray-300 hover:border-blue-500 rounded-xl p-6 text-center mb-6 transition-colors cursor-pointer bg-gray-50 hover:bg-blue-50/20">
                <input
                    type="file"
                    accept="image/*"
                    onChange={handleFileChange}
                    disabled={uploading}
                    className="hidden"
                    id="photo-upload"
                />
                <label htmlFor="photo-upload" className="cursor-pointer block">
                    {uploading ? (
                        <div className="flex flex-col items-center">
                            <Loader2 className="w-8 h-8 text-blue-600 animate-spin mb-2" />
                            <p className="text-sm font-medium text-gray-700">Uploading directly to S3...</p>
                        </div>
                    ) : (
                        <div className="flex flex-col items-center">
                            <Upload className="w-8 h-8 text-gray-400 mb-2" />
                            <p className="text-sm font-medium text-gray-700">Click to select photo</p>
                            <p className="text-xs text-gray-400 mt-1">PNG, JPG up to 10MB</p>
                        </div>
                    )}
                </label>
            </div>

            {state.evidenceKeys.length > 0 && (
                <div className="mb-6 space-y-2">
                    <p className="text-xs font-semibold text-gray-500 uppercase">Uploaded Attachments ({state.evidenceKeys.length})</p>
                    {state.evidenceKeys.map((key, idx) => (
                        <div key={idx} className="flex items-center gap-2 text-xs bg-green-50 text-green-800 border border-green-200 p-2.5 rounded-lg">
                            <ImageIcon className="w-4 h-4 text-green-600 flex-shrink-0" />
                            <span className="truncate flex-1">{key}</span>
                            <Check className="w-4 h-4 text-green-600 flex-shrink-0" />
                        </div>
                    ))}
                </div>
            )}

            <div className="flex justify-between gap-3">
                <button
                    onClick={prevStep}
                    className="flex-1 border border-gray-300 hover:bg-gray-50 text-gray-700 py-2 rounded-lg text-sm font-medium flex items-center justify-center gap-1"
                >
                    <ArrowLeft className="w-4 h-4" /> Back
                </button>
                <button
                    onClick={nextStep}
                    className="flex-1 bg-blue-600 hover:bg-blue-700 text-white py-2 rounded-lg text-sm font-medium flex items-center justify-center gap-1"
                >
                    Review Return <ArrowRight className="w-4 h-4" />
                </button>
            </div>
        </div>
    );
};
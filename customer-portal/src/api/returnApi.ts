import axios from 'axios';
import type { PresignedUrlResponse, ReturnSubmissionResponse } from '../types';

export const api = axios.create({
    baseURL: '/api/v1',
    headers: {
        'Content-Type': 'application/json',
    },
});

// 1. Get temporary PUT URL from returns-service via Gateway
export const getPresignedUrl = async (orderId: string, file: File): Promise<PresignedUrlResponse> => {
    const response = await api.post<PresignedUrlResponse>('/returns/evidence/presigned-url', {
        orderId,
        fileName: file.name,
        contentType: file.type,
    });

    // Normalize internal container hosts so browser can connect to localhost:4566
    const normalizedUploadUrl = response.data.uploadUrl
        .replace('localstack:4566', 'localhost:4566')
        .replace('s3.localhost.localstack.cloud:4566', 'localhost:4566')
        .replace('172.17.0.1:4566', 'localhost:4566');

    return {
        ...response.data,
        uploadUrl: normalizedUploadUrl,
    };
};

// 2. Direct binary PUT upload to S3 (LocalStack)
export const uploadImageToS3 = async (uploadUrl: string, file: File): Promise<void> => {
    await axios.put(uploadUrl, file, {
        headers: {
            'Content-Type': file.type,
        },
    });
};

// 3. Submit final return request with S3 evidence keys
export const submitReturnRequest = async (payload: {
    orderId: string;
    sku: string;
    item: string;
    customerEmail: string;
    quantity: number;
    zipCode: string;
    reason: string;
    evidenceKeys: string[];
}): Promise<ReturnSubmissionResponse> => {
    const response = await api.post<ReturnSubmissionResponse>('/returns', payload);
    return response.data;
};
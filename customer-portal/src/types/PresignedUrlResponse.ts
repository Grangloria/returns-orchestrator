export interface PresignedUrlResponse {
    uploadUrl: string;
    s3Key: string;
    expiresInSeconds: number;
}
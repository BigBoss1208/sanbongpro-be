# Pitch images through public HTTPS URLs

Set `MINIO_ENABLED=false` on Render to run without a MinIO account. The default
remains `true` for existing storage deployments. No database migration is needed;
the validated URL is stored in the existing `san.hinh_anh` column (255 characters).

In Admin → fields → add/edit, paste a public HTTPS image URL and choose
**Dùng URL ảnh**. The browser must successfully load/decode the image before
replacing the selected reference. Save the field to persist it. An invalid URL or
failed image load leaves the old image intact. The server validates the reference
again and never fetches an administrator-supplied URL.

Use a stable URL with a domain name, an image path, no credentials, query string,
fragment, or nonstandard port. Local addresses and literal IP addresses are not
accepted. URLs must remain publicly accessible; remote deletion or hotlink rules
can prevent later display. This feature references an already hosted image; it
does not upload image bytes to the Backend or a storage provider.

Existing object-key images still need their original configured storage. With
MinIO disabled, storage upload/download operations return HTTP 503; URL images
remain usable. The public image endpoint redirects HTTPS references directly.

VNPay, JWT, PostgreSQL and payment email behavior are unchanged. A deployment
still needs its own valid VNPay Sandbox configuration; do not reuse group or
teacher infrastructure or credentials.

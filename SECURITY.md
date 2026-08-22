# Security Policy

**InvisusNova** takes the security and privacy of **PrivaDoT** users extremely seriously. As an open-source, privacy-first application, we strive to maintain the highest standards of code integrity and defense against surveillance techniques.

---

## 🔒 Supported Versions

We provide security updates and patches for the following versions of PrivaDoT:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

---

## 🛡️ Security & Privacy Guarantees

PrivaDoT is engineered around zero-trust and zero-telemetry fundamentals:
- **No Internet Access:** The application strictly omits `android.permission.INTERNET`. It cannot physically transmit data over any network interface.
- **Hardware-Backed Encryption at Rest:** All local audit logs stored in the SQLite database are encrypted via **SQLCipher (AES-256)** using a key managed by the **Android KeyStore (TEE / StrongBox)**.
- **Forensic-Grade Erasure:** Deleting logs triggers `PRAGMA secure_delete = ON` and `VACUUM` to overwrite disk sectors with zeroes.

---

## 🚨 Reporting a Vulnerability

If you discover a security vulnerability or potential privacy leak (e.g., bypass in sensor detection, cryptographic weakness, or unintended IPC leak), please report it responsibly:

### How to Report:
1. **GitHub Security Advisory (Preferred):**
   - Submit a report privately via [GitHub Private Vulnerability Reporting](https://github.com/InvisusNova/PrivaDoT/security/advisories/new).
2. **Direct Contact:**
   - Open a confidential issue marked with the `[Security]` prefix or contact the maintainers via GitHub.

### What to Include in Your Report:
- Detailed description of the vulnerability.
- Proof of Concept (PoC) or step-by-step reproduction steps.
- Affected Android versions, OEM skins (e.g., MIUI, One UI, OxygenOS), and device models.
- Potential impact and mitigation suggestions if known.

---

## ⏱️ Response Timeline

- **Initial Response:** Within 48 hours of receiving the report.
- **Triage & Status Assessment:** Within 5 business days.
- **Patch & Public Disclosure:** A fix will be developed, tested, and released as quickly as possible. Public disclosure will happen after the patch is published.

Thank you for helping keep PrivaDoT safe and trustworthy for everyone!

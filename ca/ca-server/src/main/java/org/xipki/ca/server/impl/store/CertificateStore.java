/*
 *
 * This file is part of the XiPKI project.
 * Copyright (c) 2014 - 2015 Lijun Liao
 * Author: Lijun Liao
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation with the addition of the
 * following permission added to Section 15 as permitted in Section 7(a):
 * FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
 * THE AUTHOR LIJUN LIAO. LIJUN LIAO DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
 * OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License.
 *
 * You can be released from the requirements of the license by purchasing
 * a commercial license. Buying such a license is mandatory as soon as you
 * develop commercial activities involving the XiPKI software without
 * disclosing the source code of your own applications.
 *
 * For more information, please contact Lijun Liao at this
 * address: lijun.liao@gmail.com
 */

package org.xipki.ca.server.impl.store;

import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.util.Date;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.ca.api.OperationException;
import org.xipki.ca.api.OperationException.ErrorCode;
import org.xipki.ca.api.X509CertWithDBCertId;
import org.xipki.ca.api.publisher.X509CertificateInfo;
import org.xipki.ca.server.impl.CertRevInfoWithSerial;
import org.xipki.ca.server.impl.CertStatus;
import org.xipki.ca.server.impl.SubjectKeyProfileBundle;
import org.xipki.common.CertRevocationInfo;
import org.xipki.common.ParamChecker;
import org.xipki.common.util.LogUtil;
import org.xipki.datasource.api.DataSourceWrapper;
import org.xipki.datasource.api.exception.DataAccessException;

/**
 * @author Lijun Liao
 */

public class CertificateStore
{
    private static final Logger LOG = LoggerFactory.getLogger(CertificateStore.class);
    private final CertStoreQueryExecutor queryExecutor;

    public CertificateStore(
            final DataSourceWrapper dataSource)
    throws DataAccessException
    {
        ParamChecker.assertNotNull("dataSource", dataSource);

        this.queryExecutor = new CertStoreQueryExecutor(dataSource);
    }

    public boolean addCertificate(
            final X509CertificateInfo certInfo)
    {
        try
        {
            queryExecutor.addCert(certInfo.getIssuerCert(),
                    certInfo.getCert(),
                    certInfo.getSubjectPublicKey(),
                    certInfo.getProfileName(),
                    certInfo.getRequestor(),
                    certInfo.getUser());
        } catch (Exception e)
        {
            LOG.error("could not save certificate {}: {}. Message: {}",
                    new Object[]{certInfo.getCert().getSubject(),
                        Base64.toBase64String(certInfo.getCert().getEncodedCert()),
                        e.getMessage()});
            LOG.debug("error", e);
            return false;
        }

        return true;
    }

    public void addToPublishQueue(
            final String publisherName,
            final int certId,
            final X509CertWithDBCertId caCert)
    throws OperationException
    {
        try
        {
            queryExecutor.addToPublishQueue(publisherName, certId, caCert);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public void removeFromPublishQueue(
            final String publisherName,
            final int certId)
    throws OperationException
    {
        try
        {
            queryExecutor.removeFromPublishQueue(publisherName, certId);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public void clearPublishQueue(
            final X509CertWithDBCertId caCert,
            final String publisherName)
    throws OperationException
    {
        try
        {
            queryExecutor.clearPublishQueue(caCert, publisherName);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public long getMaxIdOfDeltaCRLCache(
            final X509CertWithDBCertId caCert)
    throws OperationException
    {
        try
        {
            return queryExecutor.getMaxIdOfDeltaCRLCache(caCert);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public void clearDeltaCRLCache(
            final X509CertWithDBCertId caCert,
            final long maxId)
    throws OperationException
    {
        try
        {
            queryExecutor.clearDeltaCRLCache(caCert, maxId);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public X509CertWithRevocationInfo revokeCertificate(
            final X509CertWithDBCertId caCert,
            final BigInteger serialNumber,
            final CertRevocationInfo revInfo,
            final boolean force,
            final boolean publishToDeltaCRLCache)
    throws OperationException
    {
        try
        {
            X509CertWithRevocationInfo revokedCert = queryExecutor.revokeCert(
                    caCert, serialNumber, revInfo, force, publishToDeltaCRLCache);
            if(revokedCert == null)
            {
                LOG.info("could not revoke non-existing certificate issuer='{}', serialNumber={}",
                    caCert.getSubject(), serialNumber);
            }
            else
            {
                LOG.info("revoked certificate issuer='{}', serialNumber={}", caCert.getSubject(), serialNumber);
            }

            return revokedCert;
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public X509CertWithDBCertId unrevokeCertificate(
            final X509CertWithDBCertId caCert,
            final BigInteger serialNumber,
            final boolean force,
            final boolean publishToDeltaCRLCache)
    throws OperationException
    {
        try
        {
            X509CertWithDBCertId unrevokedCert = queryExecutor.unrevokeCert(
                    caCert, serialNumber, force, publishToDeltaCRLCache);
            if(unrevokedCert == null)
            {
                LOG.info("could not unrevoke non-existing certificate issuer='{}', serialNumber={}",
                    caCert.getSubject(), serialNumber);
            }
            else
            {
                LOG.info("unrevoked certificate issuer='{}', serialNumber={}", caCert.getSubject(), serialNumber);
            }

            return unrevokedCert;
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    X509CertWithDBCertId getCert(
            final X509CertWithDBCertId caCert,
            final BigInteger serialNumber)
    throws OperationException
    {
        try
        {
            return queryExecutor.getCert(caCert, serialNumber);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public void removeCertificate(
            final X509CertWithDBCertId caCert,
            final BigInteger serialNumber)
    throws OperationException
    {
        try
        {
            queryExecutor.removeCertificate(caCert, serialNumber);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public boolean addCRL(
            final X509CertWithDBCertId caCert,
            final X509CRL crl)
    {
        try
        {
            queryExecutor.addCRL(caCert, crl);
            return true;
        } catch (Exception e)
        {
            LOG.error("could not add CRL ca={}, thisUpdate={}: {}, ",
                new Object[]{caCert.getSubject(),
                    crl.getThisUpdate(), e.getMessage()});
            LOG.debug("Exception", e);
            return false;
        }
    }

    public boolean hasCRL(
            final X509CertWithDBCertId caCert)
    throws OperationException
    {
        try
        {
            return queryExecutor.hasCRL(caCert);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public int getMaxCRLNumber(
            final X509CertWithDBCertId caCert)
    throws OperationException
    {
        try
        {
            return queryExecutor.getMaxCrlNumber(caCert);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public long getThisUpdateOfCurrentCRL(
            final X509CertWithDBCertId caCert)
    throws OperationException
    {
        try
        {
            return queryExecutor.getThisUpdateOfCurrentCRL(caCert);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public byte[] getEncodedCRL(
            final X509CertWithDBCertId caCert,
            final BigInteger crlNumber)
    {
        try
        {
            return queryExecutor.getEncodedCRL(caCert, crlNumber);
        } catch (Exception e)
        {
            LOG.error("could not get CRL ca={}: error message: {}",
                    caCert.getSubject(),
                    e.getMessage());
            LOG.debug("Exception", e);
            return null;
        }
    }

    public int cleanupCRLs(
            final X509CertWithDBCertId caCert,
            final int numCRLs)
    {
        try
        {
            return queryExecutor.cleanupCRLs(caCert, numCRLs);
        } catch (Exception e)
        {
            LOG.error("could not cleanup CRLs ca={}: error message: {}",
                    caCert.getSubject(),
                    e.getMessage());
            LOG.debug("Exception", e);
            return 0;
        }
    }

    public boolean certIssuedForSubject(
            final X509CertWithDBCertId caCert,
            final String sha1FpSubject)
    throws OperationException
    {
        try
        {
            return queryExecutor.certIssuedForSubject(caCert, sha1FpSubject);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public CertStatus getCertStatusForSubject(
            final X509CertWithDBCertId caCert,
            final X500Principal subject)
    {
        try
        {
            return queryExecutor.getCertStatusForSubject(caCert, subject);
        } catch (DataAccessException e)
        {
            LOG.error("queryExecutor.getCertStatusForSubject. DataAccessException: {}", e.getMessage());
            LOG.debug("queryExecutor.getCertStatusForSubject", e);
            return CertStatus.Unknown;
        }
    }

    public CertStatus getCertStatusForSubject(
            final X509CertWithDBCertId caCert,
            final X500Name subject)
    {
        try
        {
            return queryExecutor.getCertStatusForSubject(caCert, subject);
        } catch (DataAccessException e)
        {
            final String message = "queryExecutor.getCertStatusForSubject";
            if(LOG.isErrorEnabled())
            {
                LOG.error(LogUtil.buildExceptionLogFormat(message), e.getClass().getName(), e.getMessage());
            }
            LOG.debug(message, e);
            return CertStatus.Unknown;
        }
    }

    /**
     * Returns the first serial number ascend sorted {@code numEntries} revoked certificates
     * which are not expired at {@code notExpiredAt} and their serial numbers are not less than
     * {@code startSerial}.
     * @param caCert
     * @param notExpiredAt
     * @param startSerial
     * @param numEntries
     * @return
     * @throws DataAccessException
     */
    public List<CertRevInfoWithSerial> getRevokedCertificates(
            final X509CertWithDBCertId caCert,
            final Date notExpiredAt,
            final BigInteger startSerial,
            final int numEntries,
            final boolean onlyCACerts,
            final boolean onlyUserCerts)
    throws OperationException
    {
        try
        {
            return queryExecutor.getRevokedCertificates(caCert, notExpiredAt, startSerial, numEntries,
                    onlyCACerts, onlyUserCerts);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public List<CertRevInfoWithSerial> getCertificatesForDeltaCRL(
            final X509CertWithDBCertId caCert,
            final BigInteger startSerial,
            final int numEntries,
            final boolean onlyCACerts,
            final boolean onlyUserCerts)
    throws OperationException
    {
        try
        {
            return queryExecutor.getCertificatesForDeltaCRL(caCert, startSerial, numEntries,
                    onlyCACerts, onlyUserCerts);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public List<BigInteger> getCertSerials(
            final X509CertWithDBCertId caCert,
            final Date notExpiredAt,
            final BigInteger startSerial,
            final int numEntries,
            final boolean onlyRevoked,
            final boolean onlyCACerts,
            final boolean onlyUserCerts)
    throws OperationException
    {
        try
        {
            return queryExecutor.getSerialNumbers(caCert, notExpiredAt, startSerial, numEntries, onlyRevoked,
                    onlyCACerts, onlyUserCerts);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public List<BigInteger> getExpiredCertSerials(
            final X509CertWithDBCertId caCert,
            final long expiredAt,
            final int numEntries,
            final String certprofile,
            final String userLike)
    throws OperationException
    {
        try
        {
            return queryExecutor.getExpiredSerialNumbers(caCert, expiredAt, numEntries, certprofile, userLike);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public int getNumOfExpiredCerts(
            final X509CertWithDBCertId caCert,
            final long expiredAt,
            final String certprofile,
            final String userLike)
    throws OperationException
    {
        try
        {
            return queryExecutor.getNumOfExpiredCerts(caCert, expiredAt, certprofile, userLike);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public List<Integer> getPublishQueueEntries(
            final X509CertWithDBCertId caCert,
            final String publisherName,
            final int numEntries)
    throws OperationException
    {
        try
        {
            return queryExecutor.getPublishQueueEntries(caCert, publisherName, numEntries);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public X509CertWithRevocationInfo getCertWithRevocationInfo(
            final X509CertWithDBCertId caCert,
            final BigInteger serial)
    throws OperationException
    {
        try
        {
            return queryExecutor.getCertWithRevocationInfo(caCert, serial);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public X509CertificateInfo getCertificateInfoForSerial(
            final X509CertWithDBCertId caCert,
            final BigInteger serial)
    throws OperationException, CertificateException
    {
        try
        {
            return queryExecutor.getCertificateInfo(caCert, serial);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Long getGreatestSerialNumber(
            final X509CertWithDBCertId caCert)
    throws OperationException
    {
        try
        {
            return queryExecutor.getGreatestSerialNumber(caCert);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public boolean isHealthy()
    {
        return queryExecutor.isHealthy();
    }

    public SubjectKeyProfileBundle getLatestCert(
            final X509CertWithDBCertId caCert,
            final String subjectFp,
            final String keyFp,
            final String profile)
    throws OperationException
    {
        try
        {
            return queryExecutor.getLatestCert(caCert, subjectFp, keyFp, profile);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public boolean isCertForSubjectIssued(
            final X509CertWithDBCertId caCert,
            final String subjectFp)
    throws OperationException
    {
        return isCertForSubjectIssued(caCert, subjectFp, null);
    }

    public boolean isCertForSubjectIssued(
            final X509CertWithDBCertId caCert,
            final String subjectFp,
            final String profile)
    throws OperationException
    {
        try
        {
            return queryExecutor.isCertForSubjectIssued(caCert, subjectFp, profile);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public boolean isCertForKeyIssued(
            final X509CertWithDBCertId caCert,
            final String keyFp)
    throws OperationException
    {
        return isCertForKeyIssued(caCert, keyFp, null);
    }

    public boolean isCertForKeyIssued(
            final X509CertWithDBCertId caCert,
            final String keyFp,
            final String profile)
    throws OperationException
    {
        try
        {
            return queryExecutor.isCertForKeyIssued(caCert, keyFp, profile);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public X509CertificateInfo getCertificateInfoForId(
            final X509CertWithDBCertId caCert,
            final int certId)
    throws OperationException, CertificateException
    {
        try
        {
            return queryExecutor.getCertForId(caCert, certId);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public X509CertWithDBCertId getCertForId(
            final int certId)
    throws OperationException
    {
        try
        {
            return queryExecutor.getCertForId(certId);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public String getLatestSN(X500Name nameWithSN)
    throws OperationException
    {
        try
        {
            return queryExecutor.getLatestSN(nameWithSN);
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public Long getNotBeforeOfFirstCertStartsWithCN(
            final String commonName,
            final String profileName)
    throws OperationException
    {
        try
        {
            return queryExecutor.getNotBeforeOfFirstCertStartsWithCN(commonName, profileName);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public boolean containsCACertificates(
            final X509CertWithDBCertId caCert)
    throws OperationException
    {
        try
        {
            return queryExecutor.containsCertificates(caCert, false);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public boolean containsUserCertificates(
            final X509CertWithDBCertId caCert)
    throws OperationException
    {
        try
        {
            return queryExecutor.containsCertificates(caCert, true);
        } catch (DataAccessException e)
        {
            LOG.debug("DataAccessException", e);
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public long nextSerial(
            final X509CertWithDBCertId caCert,
            final String seqName)
    throws OperationException
    {
        try
        {
            return queryExecutor.nextSerial(caCert, seqName);
        } catch (DataAccessException e)
        {
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public void commitNextSerialIfLess(
            final String caName,
            final long nextSerial)
    throws OperationException
    {
        try
        {
            queryExecutor.commitNextSerialIfLess(caName, nextSerial);
        } catch (DataAccessException e)
        {
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public void markMaxSerial(
            final X509CertWithDBCertId caCert,
            final String seqName)
    throws OperationException
    {
        try
        {
            queryExecutor.markMaxSerial(caCert, seqName);
        } catch (DataAccessException e)
        {
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public void commitNextCrlNo(
            final String caName,
            final int nextCrlNo)
    throws OperationException
    {
        try
        {
            queryExecutor.commitNextCrlNoIfLess(caName, nextCrlNo);
        } catch (DataAccessException e)
        {
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public void addCa(
            final X509CertWithDBCertId caCert)
    throws OperationException
    {
        try
        {
            queryExecutor.addCa(caCert);
        } catch (DataAccessException e)
        {
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public void addRequestorName(
            final String name)
    throws OperationException
    {
        try
        {
            queryExecutor.addRequestorName(name);
        } catch (DataAccessException e)
        {
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public void addPublisherName(
            final String name)
    throws OperationException
    {
        try
        {
            queryExecutor.addPublisherName(name);
        } catch (DataAccessException e)
        {
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public void addCertprofileName(
            final String name)
    throws OperationException
    {
        try
        {
            queryExecutor.addCertprofileName(name);
        } catch (DataAccessException e)
        {
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public boolean addCertInProcess(
            final String fpKey,
            final String fpSubject)
    throws OperationException
    {
        try
        {
            return queryExecutor.addCertInProcess(fpKey, fpSubject);
        } catch (DataAccessException e)
        {
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public void delteCertInProcess(
            final String fpKey,
            final String fpSubject)
    throws OperationException
    {
        try
        {
            queryExecutor.deleteCertInProcess(fpKey, fpSubject);
        } catch (DataAccessException e)
        {
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }

    public void deleteCertsInProcessOlderThan(
            final Date time)
    throws OperationException
    {
        try
        {
            queryExecutor.deleteCertsInProcessOlderThan(time);
        } catch (DataAccessException e)
        {
            throw new OperationException(ErrorCode.DATABASE_FAILURE, e.getMessage());
        } catch (RuntimeException e)
        {
            throw new OperationException(ErrorCode.SYSTEM_FAILURE, e.getMessage());
        }
    }
}

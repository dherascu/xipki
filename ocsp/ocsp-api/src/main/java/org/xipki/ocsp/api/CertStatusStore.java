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

package org.xipki.ocsp.api;

import java.math.BigInteger;
import java.util.Set;

import org.xipki.audit.api.AuditLoggingService;
import org.xipki.audit.api.AuditLoggingServiceRegister;
import org.xipki.common.CertRevocationInfo;
import org.xipki.common.HashAlgoType;
import org.xipki.common.ParamChecker;
import org.xipki.datasource.api.DataSourceWrapper;

/**
 * @author Lijun Liao
 */

public abstract class CertStatusStore
{
    public abstract Set<IssuerHashNameAndKey> getIssuerHashNameAndKeys();

    public abstract boolean canResolveIssuer(
            HashAlgoType hashAlgo,
            byte[] issuerNameHash,
            byte[] issuerKeyHash);

    public abstract CertStatusInfo getCertStatus(
            HashAlgoType hashAlgo,
            byte[] issuerNameHash,
            byte[] issuerKeyHash,
            BigInteger serialNumber,
            boolean includeCertHash,
            HashAlgoType certHashAlg,
            CertprofileOption certprofileOption)
    throws CertStatusStoreException;

    public abstract void init(
            String conf,
            DataSourceWrapper datasource)
    throws CertStatusStoreException;

    public abstract CertRevocationInfo getCARevocationInfo(
            HashAlgoType hashAlgo,
            byte[] issuerNameHash,
            byte[] issuerKeyHash);

    public abstract void shutdown()
    throws CertStatusStoreException;

    public abstract boolean isHealthy();

    protected static final long DAY = 24L * 60 * 60 * 1000;

    private final String name;
    protected boolean unknownSerialAsGood;
    protected int retentionInterval;
    protected boolean includeArchiveCutoff;
    protected boolean includeCrlID;

    protected AuditLoggingServiceRegister auditServiceRegister;

    protected CertStatusStore(
            final String name)
    {
        ParamChecker.assertNotBlank("name", name);
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setAuditServiceRegister(
            final AuditLoggingServiceRegister auditServiceRegister)
    {
        this.auditServiceRegister = auditServiceRegister;
    }

    public AuditLoggingService getAuditLoggingService()
    {
        return auditServiceRegister == null ? null : auditServiceRegister.getAuditLoggingService();
    }

    public boolean isUnknownSerialAsGood()
    {
        return unknownSerialAsGood;
    }

    public void setUnknownSerialAsGood(
            final boolean unknownSerialAsGood)
    {
        this.unknownSerialAsGood = unknownSerialAsGood;
    }

    public boolean isIncludeArchiveCutoff()
    {
        return includeArchiveCutoff;
    }

    public void setIncludeArchiveCutoff(
            final boolean includeArchiveCutoff)
    {
        this.includeArchiveCutoff = includeArchiveCutoff;
    }

    public int getRetentionInterval()
    {
        return retentionInterval;
    }

    public void setRetentionInterval(
            final int retentionInterval)
    {
        this.retentionInterval = retentionInterval;
    }

    public boolean isIncludeCrlID()
    {
        return includeCrlID;
    }

    public void setIncludeCrlID(
            final boolean includeCrlID)
    {
        this.includeCrlID = includeCrlID;
    }

}

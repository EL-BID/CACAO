/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.entities;

import static org.springframework.data.elasticsearch.annotations.FieldType.Date;
import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.idb.cacao.api.Views;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import com.fasterxml.jackson.annotation.JsonView;

/**
 * Password reset token
 * 
 * @author Gustavo Figueiredo
 *
 */
@Document(indexName="cacao_pwd_reset")
public class PasswordResetToken {

	public static final int EXPIRATION = 60 * 24; // 24 hours
    
	@Id
	@JsonView(Views.Authority.class)
    private String id;
 
	@Field(type=Keyword)
    private String token;
 
	@Field(type=Keyword)
	@JsonView(Views.Authority.class)
    private String userId;
 
	@JsonView(Views.Authority.class)
	@Field(type=Date, store = true, format = DateFormat.date_time)
    private OffsetDateTime expiryDate;
    
    public PasswordResetToken() {
        super();
    }

    public PasswordResetToken(final String token) {
        super();

        this.token = token;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    public PasswordResetToken(final String token, final User user) {
        super();

        this.token = token;
        this.userId = user.getId();
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public OffsetDateTime getExpiryDate() {
		return expiryDate;
	}

	public void setExpiryDate(OffsetDateTime expiryDate) {
		this.expiryDate = expiryDate;
	}

    private OffsetDateTime calculateExpiryDate(final int expiryTimeInMinutes) {
    	return OffsetDateTime.now().plus(expiryTimeInMinutes, ChronoUnit.MINUTES);
    }

    public void updateToken(final String token) {
        this.token = token;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getExpiryDate() == null) ? 0 : getExpiryDate().hashCode());
        result = prime * result + ((getToken() == null) ? 0 : getToken().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PasswordResetToken other = (PasswordResetToken) obj;
        if (getExpiryDate() == null) {
            if (other.getExpiryDate() != null) {
                return false;
            }
        } else if (!getExpiryDate().equals(other.getExpiryDate())) {
            return false;
        }
        if (getToken() == null) {
            if (other.getToken() != null) {
                return false;
            }
        } else if (!getToken().equals(other.getToken())) {
            return false;
        }
        if (getUserId() == null) {
            if (other.getUserId() != null) {
                return false;
            }
        } else if (!getUserId().equals(other.getUserId())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Token [String=").append(token).append("]").append("[Expires").append(expiryDate).append("]");
        return builder.toString();
    }

}

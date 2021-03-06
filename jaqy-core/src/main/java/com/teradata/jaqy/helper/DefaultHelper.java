/*
 * Copyright (c) 2017-2021 Teradata
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.teradata.jaqy.helper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.*;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;

import com.teradata.jaqy.Globals;
import com.teradata.jaqy.JaqyException;
import com.teradata.jaqy.JaqyInterpreter;
import com.teradata.jaqy.connection.*;
import com.teradata.jaqy.interfaces.JaqyHelper;
import com.teradata.jaqy.interfaces.JaqyResultSet;
import com.teradata.jaqy.resultset.InMemoryResultSet;
import com.teradata.jaqy.schema.*;
import com.teradata.jaqy.typehandler.TypeHandler;
import com.teradata.jaqy.typehandler.TypeHandlerRegistry;
import com.teradata.jaqy.utils.*;

/**
 * @author  Heng Yuan
 */
public class DefaultHelper implements JaqyHelper
{
    private final JaqyConnection m_conn;
    private final Globals m_globals;
    private final JdbcFeatures m_features;

    private SimpleQuery m_catalogQuery;
    private SimpleQuery m_schemaQuery;
    private SimpleQuery m_tableSchemaQuery;
    private MessageFormat m_tableSchemaFormat;
    private MessageFormat m_tableColumnFormat;

    private TypeMap m_typeMap;
    private TypeMap m_importTypeMap;
    private Map<Integer, TypeInfo> m_customTypeMap;
    private Map<Integer, TypeInfo> m_customImportTypeMap;

    public DefaultHelper (JdbcFeatures features, JaqyConnection conn, Globals globals)
    {
        m_features = features;
        m_conn = conn;
        m_globals = globals;
    }

    @Override
    public JdbcFeatures getFeatures ()
    {
        return m_features;
    }

    public Globals getGlobals ()
    {
        return m_globals;
    }

    @Override
    public JaqyResultSet getResultSet (ResultSet rs, JaqyInterpreter interpreter) throws SQLException
    {
        return new JaqyDefaultResultSet (rs, this);
    }

    @Override
    public Object getObject (JaqyResultSet rs, int index) throws SQLException
    {
        return rs.getObjectInternal (index);
    }

    @Override
    public JaqyConnection getConnection ()
    {
        return m_conn;
    }

    @Override
    public JaqyStatement createStatement (boolean forwardOnly) throws SQLException
    {
        Connection conn = m_conn.getConnection ();
        if (m_features.forwardOnlyRS || forwardOnly)
            return new JaqyStatement (conn.createStatement (), m_conn);
        else
        {
            try
            {
                return new JaqyStatement (conn.createStatement (ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY), m_conn);
            }
            catch (SQLException ex)
            {
                // we are catching all SQLException rather than just
                // SQLFeatureNotSupportedException because some JDBC drivers
                // throw SQLException in all cases.
                getFeatures().forwardOnlyRS = true;
                return new JaqyStatement (conn.createStatement (), m_conn);
            }
        }
    }

    @Override
    public JaqyPreparedStatement preparedStatement (String sql) throws SQLException
    {
        Connection conn = m_conn.getConnection ();
        return new JaqyPreparedStatement (conn.prepareStatement (sql), m_conn);
    }

    private void createTypeMap () throws SQLException
    {
        if (m_typeMap != null)
            return;
        m_typeMap = SchemaUtils.getTypeMap (m_conn);
        if (m_typeMap != null && m_customTypeMap != null)
        {
            m_typeMap.setCustomMap (m_customTypeMap);
        }
    }

    private void createImportTypeMap () throws SQLException
    {
        if (m_importTypeMap != null)
            return;
        m_importTypeMap = SchemaUtils.getTypeMap (m_conn);
        if (m_importTypeMap != null)
        {
            if (m_customTypeMap != null)
            {
                m_importTypeMap.setCustomMap (m_customTypeMap);
            }
            if (m_customImportTypeMap != null)
            {
                m_importTypeMap.setCustomMap (m_customImportTypeMap);
            }
        }
    }

    @Override
    public TypeMap getTypeMap (boolean forImport) throws SQLException
    {
        if (forImport)
        {
            createImportTypeMap ();
            return m_importTypeMap;
        }
        else
        {
            createTypeMap ();
            return m_typeMap;
        }
    }

    private String getCatalogInternal () throws SQLException
    {
        if (m_features.noCatalog)
            return null;
        try
        {
            return m_conn.getConnection ().getCatalog ();
        }
        catch (SQLFeatureNotSupportedException ex)
        {
            m_features.noCatalog = true;
            return null;
        }
        catch (SQLException ex)
        {
            throw ex;
        }
        catch (Throwable t)
        {
            m_features.noCatalog = true;
            return null;
        }
    }

    private String getSchemaInternal () throws SQLException
    {
        if (m_features.noSchema)
            return null;
        try
        {
            return m_conn.getConnection ().getSchema ();
        }
        catch (SQLFeatureNotSupportedException ex)
        {
            getFeatures().noSchema = true;
            return null;
        }
        catch (SQLException ex)
        {
            throw ex;
        }
        catch (Throwable t)
        {
            m_features.noSchema = true;
            return null;
        }
    }

    @Override
    public String getCatalog (JaqyInterpreter interpreter) throws SQLException
    {
        if (m_catalogQuery == null)
            return getCatalogInternal ();
        return interpreter.getQueryString (m_catalogQuery.sql, m_catalogQuery.columnIndex);
    }

    @Override
    public String getSchema (JaqyInterpreter interpreter) throws SQLException
    {
        if (m_schemaQuery == null)
            return getSchemaInternal ();
        return interpreter.getQueryString (m_schemaQuery.sql, m_schemaQuery.columnIndex);
    }

    @Override
    public String getURL () throws SQLException
    {
        String url = m_conn.getMetaData ().getURL ();
        if (url.startsWith ("jdbc:"))
            url = url.substring (5);
        return url;
    }

    @Override
    public String getPath () throws SQLException
    {
        boolean hasUser = false;
        StringBuilder buffer = new StringBuilder ();
        String user = m_conn.getMetaData ().getUserName ();
        if (user != null && user.length () > 0)
        {
            hasUser = true;
            buffer.append (user);
        }

        boolean hasHost = false;
        String url = m_conn.getMetaData ().getURL ();
        int start = url.indexOf ("//");
        if (start > 0)
        {
            start += 2;
            hasHost = true;
            int end = url.indexOf (':', start);
            if (end > 0)
                url = url.substring (start, end);
            else
                url = url.substring (start);
            end = url.indexOf ('/');
            if (end < 0)
                end = url.length ();
            String host = url.substring (0, end);
            if (hasUser)
                buffer.append (" @ ");
            buffer.append (host);
        }

        String path = null;
        String catalog = getCatalogInternal ();
        String schema = getSchemaInternal ();
        if (catalog == null || catalog.length () == 0)
        {
            path = schema;
        }
        else
        {
            if (schema == null || schema.length () == 0)
                path = catalog;
            else
                path = catalog + m_conn.getCatalogSeparator () + schema;
        }
        if (path != null)
        {
            if (hasUser || hasHost)
            {
                buffer.append (" - ");
            }
            buffer.append (path);
        }
        if (buffer.length () > 0)
            return buffer.toString ();
        return getURL ();
    }

    @Override
    public boolean isJsonColumn (JaqyResultSetMetaData meta, int column) throws SQLException
    {
        if ("JSON".equalsIgnoreCase (meta.getColumnTypeName (column)))
            return true;
        return false;
    }

    @Override
    public TypeHandler getTypeHandler (JaqyResultSet rs, int column) throws SQLException
    {
        return TypeHandlerRegistry.getTypeHandler (rs.getMetaData ().getColumnType (column));
    }

    @Override
    public Array createArrayOf (ParameterInfo paramInfo, Object[] elements) throws SQLException
    {
        return m_conn.createArrayOf ("VARCHAR", elements);
    }

    @Override
    public Struct createStruct (ParameterInfo paramInfo, Object[] elements) throws SQLException
    {
        return m_conn.createStruct (paramInfo.typeName, elements);
    }

    @Override
    public String getTypeName (int type, int precision, int scale, boolean exact, boolean forImport) throws SQLException
    {
        TypeMap typeMap = getTypeMap (forImport);
        if (typeMap == null)
            return null;
        return typeMap.getTypeName (type, precision, scale, exact);
    }

    @Override
    public String getTypeName (BasicColumnInfo columnInfo, boolean forImport) throws SQLException
    {
        if (columnInfo.typeName == null)
        {
            TypeMap typeMap = getTypeMap (forImport);
            if (typeMap == null)
                return null;
            return typeMap.getTypeName (columnInfo.type, columnInfo.precision, columnInfo.scale, true);
        }

        switch (columnInfo.type)
        {
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.NVARCHAR:
            case Types.NCHAR:
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.CLOB:
            case Types.NCLOB:
            case Types.BLOB:
            {
                int precision = columnInfo.precision;
                /* Prevent the case of VARCHAR(0)
                 */
                if (precision == 0)
                {
                    precision = 1;
                }
                TypeMap typeMap = getTypeMap (forImport);
                if (typeMap != null)
                {
                    String preciseName = typeMap.getTypeName (columnInfo.type, columnInfo.precision, columnInfo.scale, true);
                    if (preciseName != null && TypeMap.isSameType (preciseName, columnInfo.typeName))
                    {
                        return preciseName;
                    }
                }
                /* If the size is quite big, it may be the default size.
                 * In that case, just return the type name itself.
                 *
                 * If the type name contains space, it is difficult to
                 * tell where to put the precision.
                 */
                if (precision < 0x7fff0000 && columnInfo.typeName.indexOf (' ') < 0)
                {
                    return columnInfo.typeName + "(" + precision + ")";
                }
                return columnInfo.typeName;
            }
            case Types.DECIMAL:
            case Types.NUMERIC:
            {
                return columnInfo.typeName + "(" + columnInfo.precision + "," + columnInfo.scale + ")";
            }
            default:
                return columnInfo.typeName;
        }
    }

    @Override
    public boolean checkTableExists (String tableName, JaqyInterpreter interpreter) throws SQLException
    {
        return SessionUtils.checkTableExists (interpreter.getSession (), tableName);
    }

    /**
     * Guess the schema for a table.
     */
    @Override
    public String getTableSchema (String tableName, JaqyInterpreter interpreter) throws Exception
    {
        if (m_tableSchemaQuery != null)
        {
            String value = interpreter.getQueryString (m_tableSchemaFormat.format (new Object[]{ tableName }), m_tableSchemaQuery.columnIndex);
            if (value == null || value.length () == 0)
                throw ExceptionUtils.getTableNotFound ();
            return value;
        }

        return SessionUtils.getTableSchema (tableName, interpreter.getSession (), interpreter);
    }

    @Override
    public JaqyResultSet getTableColumns (String tableName, JaqyInterpreter interpreter) throws Exception
    {
        if (m_tableColumnFormat != null)
        {
            JaqyResultSet rs = interpreter.getResultSet (m_tableColumnFormat.format (new Object[]{ tableName }));
            if (rs == null)
                throw ExceptionUtils.getTableNotFound ();
            InMemoryResultSet inmemrs = (InMemoryResultSet) rs.getResultSet ();
            if (inmemrs.getRows ().size () == 0)
                throw ExceptionUtils.getTableNotFound ();
            return rs;
        }

        return SessionUtils.getTableColumns (tableName, interpreter.getSession (), interpreter);
    }

    @Override
    public int getNumColumns (String tableName, JaqyInterpreter interpreter) throws Exception
    {
        try (JaqyResultSet rs = getTableColumns (tableName, interpreter))
        {
            int count = 0;
            while (rs.next ())
                ++count;
            return count;
        }
    }

    protected void fixOtherType (BasicColumnInfo info)
    {
        if (info.className.startsWith ("java.lang."))
        {
            if ("java.lang.Integer".equals (info.className))
                info.type = Types.INTEGER;
            if ("java.lang.String".equals (info.className))
                info.type = Types.VARCHAR;
        }
        else if (info.className.startsWith ("java.sql."))
        {
            if ("java.sql.Clob".equals (info.className))
                info.type = Types.CLOB;
            if ("java.sql.NClob".equals (info.className))
                info.type = Types.NCLOB;
            if ("java.sql.Blob".equals (info.className))
                info.type = Types.BLOB;
            if ("java.sql.SQLXML".equals (info.className))
                info.type = Types.SQLXML;
        }
    }

    protected FullColumnInfo[] createElementType (int type, String typeName)
    {
        FullColumnInfo[] infos = new FullColumnInfo[1];
        infos[0] = new FullColumnInfo ();
        infos[0].type = type;
        infos[0].typeName = typeName;
        return infos;
    }

    private void setVarCharType (FullColumnInfo info)
    {
        info.type = Types.VARCHAR;
        info.nullable = ResultSetMetaData.columnNullableUnknown;
        info.displaySize = Integer.MAX_VALUE;
        info.precision = Integer.MAX_VALUE;
        info.className = "java.lang.String";
    }

    @Override
    public void fixColumnInfo (FullColumnInfo info)
    {
        if (info.type == Types.OTHER &&
            info.className != null)
        {
            fixOtherType (info);
        }
        else if (info.type == Types.STRUCT ||
                 info.type == Types.ARRAY)
        {
            /* By default, we have no way of knowing the Array / Struct
             * element types.  JDBC API unfortunately relies on actually
             * getting an instance of Array to get more information.
             * And there is basically nothing in JDBC API for getting the
             * child data.
             *
             * So the simplest approach is to treat Struct / Array
             * as array of string.
             */
            info.type = Types.ARRAY;
            info.children = createElementType (Types.VARCHAR, "varchar");
            setVarCharType (info.children[0]);
        }
    }

    @Override
    public void fixParameterInfo (ParameterInfo info)
    {
        if (info.type == Types.OTHER &&
            info.className != null)
        {
            fixOtherType (info);
        }
    }

    public void setCatalogQuery (SimpleQuery catalogQuery)
    {
        m_catalogQuery = catalogQuery;
    }

    public void setSchemaQuery (SimpleQuery schemaQuery)
    {
        m_schemaQuery = schemaQuery;
    }

    public void setTableSchemaQuery (SimpleQuery tableSchemaQuery)
    {
        m_tableSchemaQuery = tableSchemaQuery;
        if (tableSchemaQuery == null)
        {
            m_tableSchemaFormat = null;
        }
        else
        {
            m_tableSchemaFormat = new MessageFormat (tableSchemaQuery.sql);
        }
    }

    public void setTableColumnQuery (SimpleQuery tableSchemaQuery)
    {
        if (tableSchemaQuery == null)
            m_tableColumnFormat = null;
        else
            m_tableColumnFormat = new MessageFormat (tableSchemaQuery.sql);
    }

    public void setCustomTypeMap (Map<Integer, TypeInfo> map)
    {
        m_customTypeMap = map;
    }

    public void setCustomImportTypeMap (Map<Integer, TypeInfo> map)
    {
        m_customImportTypeMap = map;
    }

    @Override
    public String getQuotedIdentifier (String name) throws SQLException
    {
        DatabaseMetaData meta = m_conn.getMetaData ();
        String quote = meta.getIdentifierQuoteString ();
        return SchemaUtils.getQuotedIdentifier (name, quote);
    }

    @Override
    public void setNull (JaqyPreparedStatement stmt, int columnIndex, ParameterInfo paramInfo, JaqyInterpreter interpreter) throws Exception
    {
        stmt.setNull (columnIndex, paramInfo.type, paramInfo.typeName);
    }

    private void setBlob (JaqyPreparedStatement stmt, int columnIndex, Blob blob) throws SQLException
    {
        if (m_features.noStream)
        {
            stmt.setBytes (columnIndex, blob.getBytes (1, (int)blob.length ()));
        }
        else
        {
            setBinaryStream (stmt, columnIndex, blob.getBinaryStream (), blob.length ());
        }
    }

    private void setClob (JaqyPreparedStatement stmt, int columnIndex, Clob clob) throws SQLException
    {
        if (m_features.noStream)
        {
            stmt.setString (columnIndex, clob.getSubString (1, (int)clob.length ()));
        }
        else
        {
            setCharacterStream (stmt, columnIndex, clob.getCharacterStream (), clob.length ());
        }
    }

    @Override
    public void setObject (JaqyPreparedStatement stmt, int columnIndex, ParameterInfo paramInfo, Object o, Collection<Object> freeList, JaqyInterpreter interpreter) throws Exception
    {
        switch (paramInfo.type)
        {
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
            {
                stmt.setObject (columnIndex, o, paramInfo.type);
                return;
            }
            case Types.BLOB:
            {
                if (o instanceof Blob)
                {
                    setBlob (stmt, columnIndex, (Blob)o);
                    return;
                }
                else if (o instanceof byte[] ||
                         o instanceof ByteBuffer)
                {
                    byte[] bytes;
                    if (o instanceof byte[])
                    {
                        bytes = (byte[])o;
                    }
                    else
                    {
                        ByteBuffer bb = (ByteBuffer)o;
                        int size = bb.remaining ();
                        bytes = new byte[size];
                        bb.get (bytes);
                    }
                    if (m_features.noStream)
                    {
                        stmt.setBytes (columnIndex, bytes);
                    }
                    else
                    {
                        setBinaryStream (stmt, columnIndex, new ByteArrayInputStream (bytes), bytes.length);
                    }
                    return;
                }
                // go to the default handling.
                break;
            }
            case Types.CLOB:
            case Types.NCLOB:
            {
                if (o instanceof Clob)
                {
                    setClob (stmt, columnIndex, (Clob)o);
                    return;
                }
                else if (o instanceof CharSequence)
                {
                    if (m_features.noStream)
                    {
                        stmt.setString (columnIndex, o.toString ());
                    }
                    else
                    {
                        String str = o.toString ();
                        setCharacterStream (stmt, columnIndex, new StringReader (str), str.length ());
                    }
                    return;
                }
                // go to the default handling.
                break;
            }
            case Types.SQLXML:
            {
                if (o instanceof SQLXML)
                {
                    stmt.setSQLXML (columnIndex, (SQLXML)o);
                    return;
                }
                else if (o instanceof CharSequence)
                {
                    if (m_features.noStream)
                    {
                        stmt.setString (columnIndex, o.toString ());
                    }
                    else
                    {
                        SQLXML x = m_conn.createSQLXML ();
                        x.setString (o.toString ());
                        stmt.setSQLXML (columnIndex, x);
                        freeList.add (x);
                    }
                    return;
                }
                // go to the default handling.
                break;
            }
        }

        if (o instanceof SQLXML)
        {
            SQLXML xml = (SQLXML)o;
            if (m_features.noStream)
                stmt.setString (columnIndex, xml.getString ());
            else
            {
                // There is no length information from SQLXML object.
                stmt.setCharacterStream (columnIndex, xml.getCharacterStream ());
            }
            return;
        }
        else if (o instanceof Clob)
        {
            setClob (stmt, columnIndex, (Clob)o);
            return;
        }
        else if (o instanceof Blob)
        {
            setBlob (stmt, columnIndex, (Blob)o);
            return;
        }

        if (m_features.setObjectType)
        {
            stmt.setObject (columnIndex, o, paramInfo.type);
        }
        else
        {
            stmt.setObject (columnIndex, o);
        }
    }

    /**
     * For numbers, we want to convert them to the appropriate data type
     * to avoid the hassle that sometimes some JDBC driver error out
     * on type conversion in edge cases.
     */
    @Override
    public void setCSVObject (JaqyPreparedStatement stmt, int columnIndex, ParameterInfo paramInfo, Object o, Collection<Object> freeList, JaqyInterpreter interpreter) throws Exception
    {
        try
        {
            if (o instanceof String)
            {
                String value = o.toString ();
                switch (paramInfo.type)
                {
                    case Types.TINYINT:
                    {
                        BigDecimal dec = new BigDecimal (value);
                        setObject (stmt, columnIndex, paramInfo, dec.byteValue (), freeList, interpreter);
                        break;
                    }
                    case Types.SMALLINT:
                    {
                        BigDecimal dec = new BigDecimal (value);
                        setObject (stmt, columnIndex, paramInfo, dec.shortValue (), freeList, interpreter);
                        break;
                    }
                    case Types.INTEGER:
                    {
                        BigDecimal dec = new BigDecimal (value);
                        setObject (stmt, columnIndex, paramInfo, dec.intValue (), freeList, interpreter);
                        break;
                    }
                    case Types.BIGINT:
                    {
                        BigDecimal dec = new BigDecimal (value);
                        setObject (stmt, columnIndex, paramInfo, dec.longValueExact (), freeList, interpreter);
                        break;
                    }
                    case Types.REAL:
                    case Types.FLOAT:
                    case Types.DOUBLE:
                    {
                        BigDecimal dec = new BigDecimal (value);
                        setObject (stmt, columnIndex, paramInfo, dec.doubleValue (), freeList, interpreter);
                        break;
                    }
                    case Types.DECIMAL:
                    case Types.NUMERIC:
                    {
                        BigDecimal dec = new BigDecimal (value);
                        setObject (stmt, columnIndex, paramInfo, dec, freeList, interpreter);
                        break;
                    }
                    case Types.BINARY:
                    case Types.VARBINARY:
                    case Types.LONGVARBINARY:
                    case Types.BLOB:
                    {
                        byte[] bytes = StringUtils.getBytesFromHexString (value);
                        setObject (stmt, columnIndex, paramInfo, bytes, freeList, interpreter);
                        break;
                    }
                    case Types.SQLXML:
                    {
                        SQLXML xml = getConnection ().createSQLXML ();
                        xml.setString (value);
                        setObject (stmt, columnIndex, paramInfo, xml, freeList, interpreter);
                        break;
                    }
                    default:
                    {
                        stmt.setString (columnIndex, value);
                    }
                }
            }
            else
            {
                setObject (stmt, columnIndex, paramInfo, o, freeList, interpreter);
            }
        }
        catch (NumberFormatException ex)
        {
            throw new JaqyException ("Invalid number format: " + o, ex);
        }
    }

    @Override
    public void setBinaryStream (JaqyPreparedStatement stmt, int parameter, InputStream is, long length) throws SQLException
    {
        if (m_features.streamLength)
        {
            stmt.setBinaryStream (parameter, is, length);
        }
        else
        {
            stmt.setBinaryStream (parameter, is);
        }
    }

    @Override
    public void setCharacterStream (JaqyPreparedStatement stmt, int parameter, Reader reader, long length) throws SQLException
    {
        if (m_features.streamLength)
        {
            stmt.setCharacterStream (parameter, reader, length);
        }
        else
        {
            stmt.setCharacterStream (parameter, reader);
        }
    }

    /**
     * Because we change the number types handling in setCSVObject, we need
     * to use the right type for setNull call.
     */
    @Override
    public void setCSVNull (JaqyPreparedStatement stmt, int columnIndex, ParameterInfo paramInfo, JaqyInterpreter interpreter) throws Exception
    {
        if (TypesUtils.isNumber (paramInfo.type) ||
            TypesUtils.isBinary (paramInfo.type) ||
            paramInfo.type == Types.SQLXML)
        {
            stmt.setNull (columnIndex, paramInfo.type);
        }
        else
        {
            stmt.setNull (columnIndex, Types.VARCHAR);
        }
    }

    @Override
    public String getImportTableIndex ()
    {
        return "";
    }
}

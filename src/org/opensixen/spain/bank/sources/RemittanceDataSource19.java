 /******* BEGIN LICENSE BLOCK *****
 * Versión: GPL 2.0/CDDL 1.0/EPL 1.0
 *
 * Los contenidos de este fichero están sujetos a la Licencia
 * Pública General de GNU versión 2.0 (la "Licencia"); no podrá
 * usar este fichero, excepto bajo las condiciones que otorga dicha 
 * Licencia y siempre de acuerdo con el contenido de la presente. 
 * Una copia completa de las condiciones de de dicha licencia,
 * traducida en castellano, deberá estar incluida con el presente
 * programa.
 * 
 * Adicionalmente, puede obtener una copia de la licencia en
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Este fichero es parte del programa opensiXen.
 *
 * OpensiXen es software libre: se puede usar, redistribuir, o
 * modificar; pero siempre bajo los términos de la Licencia 
 * Pública General de GNU, tal y como es publicada por la Free 
 * Software Foundation en su versión 2.0, o a su elección, en 
 * cualquier versión posterior.
 *
 * Este programa se distribuye con la esperanza de que sea útil,
 * pero SIN GARANTÍA ALGUNA; ni siquiera la garantía implícita 
 * MERCANTIL o de APTITUD PARA UN PROPÓSITO DETERMINADO. Consulte 
 * los detalles de la Licencia Pública General GNU para obtener una
 * información más detallada. 
 *
 * TODO EL CÓDIGO PUBLICADO JUNTO CON ESTE FICHERO FORMA PARTE DEL 
 * PROYECTO OPENSIXEN, PUDIENDO O NO ESTAR GOBERNADO POR ESTE MISMO
 * TIPO DE LICENCIA O UNA VARIANTE DE LA MISMA.
 *
 * El desarrollador/es inicial/es del código es
 *  FUNDESLE (Fundación para el desarrollo del Software Libre Empresarial).
 *  Indeos Consultoria S.L. - http://www.indeos.es
 *
 * Contribuyente(s):
 *  Alejandro González <alejandro@opensixen.org> 
 *
 * Alternativamente, y a elección del usuario, los contenidos de este
 * fichero podrán ser usados bajo los términos de la Licencia Común del
 * Desarrollo y la Distribución (CDDL) versión 1.0 o posterior; o bajo
 * los términos de la Licencia Pública Eclipse (EPL) versión 1.0. Una 
 * copia completa de las condiciones de dichas licencias, traducida en 
 * castellano, deberán de estar incluidas con el presente programa.
 * Adicionalmente, es posible obtener una copia original de dichas 
 * licencias en su versión original en
 *  http://www.opensource.org/licenses/cddl1.php  y en  
 *  http://www.opensource.org/licenses/eclipse-1.0.php
 *
 * Si el usuario desea el uso de SU versión modificada de este fichero 
 * sólo bajo los términos de una o más de las licencias, y no bajo los 
 * de las otra/s, puede indicar su decisión borrando las menciones a la/s
 * licencia/s sobrantes o no utilizadas por SU versión modificada.
 *
 * Si la presente licencia triple se mantiene íntegra, cualquier usuario 
 * puede utilizar este fichero bajo cualquiera de las tres licencias que 
 * lo gobiernan,  GPL 2.0/CDDL 1.0/EPL 1.0.
 *
 * ***** END LICENSE BLOCK ***** */

package org.opensixen.spain.bank.sources;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Properties;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.opensixen.bankoperations.xml.BankNode;
import org.opensixen.model.MRemittance;
import org.opensixen.source.RemittanceDataSource;


/**
 * 
 * RemittanceDataSource19
 *
 * @author Alejandro González
 * Nexis Servicios Informáticos http://www.nexis.es
 */

public class RemittanceDataSource19 implements RemittanceDataSource{

	
	/**	Properties				*/
	private Properties p_ctx = null;
	
	/** Trx Name	*/
	private String trxName;
	
	/** Log	*/
	protected CLogger log = CLogger.getCLogger( getClass());
	
	
	/** Lineas del informe		*/
	private M_RemittanceLine[] m_RemittanceLines;
		
	/** Registro Actual */
	private int m_currentRecord = 0; 
	
	private MRemittance remittance=null;
	
	/**
	 * Contructores
	 */
	
	public RemittanceDataSource19(){}
	
	public RemittanceDataSource19 (MRemittance remit)	{
		remittance = remit;
		loadData();
	}
	

	/**
	 * Obtiene el where de la sentencia sql
	 * @return
	 */

	public String getWhere(){
		String sql=" WHERE 1=1";
		sql+=" AND r.c_remittance_id="+remittance.getC_Remittance_ID();
		
		return sql;
	}
	
	
	public void loadData() throws RuntimeException {
		
		
		// ArrayList donde guardaremos los datos de la remesa
		ArrayList<M_RemittanceLine> list = new ArrayList<M_RemittanceLine>();
		StringBuffer sql=null;

			sql = new StringBuffer ( "SELECT r.c_remittance_id,g.name as orgname,f.DUNS,r.generatedate,ba.accountno as accountno,r.executedate,bp.name as partnername,i.documentno,bpk.accountno as partneraccount,rl.grandtotal,i.dateinvoiced,r.totalamt");
			sql.append(" FROM C_Remittance r ");
			sql.append(" INNER JOIN AD_Org g ON g.ad_org_id=r.ad_org_id");
			sql.append(" INNER JOIN AD_OrgInfo f ON f.ad_org_id=g.ad_org_id");
			sql.append(" INNER JOIN C_RemittanceLine rl ON rl.c_remittance_id=r.c_remittance_id");
			sql.append(" INNER JOIN C_Invoice i ON i.c_invoice_id=rl.c_invoice_id");
			sql.append(" LEFT  JOIN C_BankAccount ba ON ba.c_bankaccount_id=r.c_bankaccount_id ");
			sql.append(" INNER JOIN C_BPartner bp ON bp.c_bpartner_id=i.c_bpartner_id");
			sql.append(" LEFT  JOIN C_BP_Bankaccount bpk ON (bpk.c_bpartner_id=bp.c_bpartner_id AND bpk.isactive='Y')");

		//Añadimos el where a la sentencia
		sql.append(getWhere());
		sql.append(" ORDER BY i.documentno");
		log.info("SQL: " + sql.toString());
		System.out.println("SQL="+sql.toString());
		try {
			PreparedStatement pstmt = DB.prepareStatement(sql.toString(), trxName);
			ResultSet rs = pstmt.executeQuery();
			
			while (rs.next())	{
				M_RemittanceLine line = new M_RemittanceLine(rs);
				list.add(line);
			}
			
			rs.close();
			pstmt.close();
			pstmt = null;
					
		}
		catch (SQLException e)	{
			throw new RuntimeException("Error e="+e);
		}
	
		
		
		// Guardamos la lista en m_RemittanceLines
		m_RemittanceLines = new M_RemittanceLine[list.size()];
		System.out.println("Total de lineas,="+list.size());
		list.toArray(m_RemittanceLines);
	}	
	
	
 
	public Object getFieldValue(String name) {
		
		if (name.toUpperCase().equals(BankNode.Data_OrgName))	{
			return m_RemittanceLines[m_currentRecord].getOrgName();
		}
		if (name.toUpperCase().equals(BankNode.Data_Duns))	{
			return m_RemittanceLines[m_currentRecord].getDUNS();
		}
		else if (name.toUpperCase().equals(BankNode.Data_GenerateDate))	{
			return m_RemittanceLines[m_currentRecord].getGenerateDate();
		}
		else if (name.toUpperCase().equals(BankNode.Data_ExecuteDate))	{
			return m_RemittanceLines[m_currentRecord].getExecuteDate();
		}
		else if (name.toUpperCase().equals(BankNode.Data_DateInvoiced))	{
			return m_RemittanceLines[m_currentRecord].getDateInvoiced();
		}
		else if (name.toUpperCase().equals(BankNode.Data_AccountNo))	{
			return m_RemittanceLines[m_currentRecord].getAccountNo();
		}
		else if (name.toUpperCase().equals(BankNode.Data_BPName))	{
			return m_RemittanceLines[m_currentRecord].getBPName();
		}
		else if (name.toUpperCase().equals(BankNode.Data_DocumentNo))	{
			return m_RemittanceLines[m_currentRecord].getDocumentNo();
		}
		else if (name.toUpperCase().equals(BankNode.Data_BPAccountNo))	{
			return m_RemittanceLines[m_currentRecord].getBPAccount();
		}
		else if (name.toUpperCase().equals(BankNode.Data_LineTotal))	{
			return m_RemittanceLines[m_currentRecord].getLineTotal();
		}
		else if (name.toUpperCase().equals(BankNode.Data_TotalAmt))	{
			return m_RemittanceLines[m_currentRecord].getTotalAmt();
		}
		else {
			System.out.println("-------------No Encuentra el valor-------------");
			//throw new Exception("No se ha podidod obtener el valor de la columna " + name);
		}
		return null;
	}
 
	/**
	 * Salta al siguiente registro del DataSource
	 */
	public boolean next()  {

		m_currentRecord++;
		
		if (m_currentRecord >= m_RemittanceLines.length )	{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Salta al anterior registro del DataSource
	 */
	public boolean previous()  {

		m_currentRecord--;
		
		if (m_currentRecord < 0 )	{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Devuelve la ultima posicion actual del datasource
	 * @return
	 */
	
	public int getCurrentRecord(){
		return m_currentRecord;
	}

	@Override
	public void setRemittance(MRemittance rem) {
		remittance=rem;
		
	}

	@Override
	public void init(MRemittance rem) {
		setRemittance(rem);
		loadData();
		
	}


}

class M_RemittanceLine	{
	
	private int MRemittanceID=0;
	
	private String OrgName;
	private String DUNS;
	private Timestamp GenerateDate;
	private Timestamp ExecuteDate;
	private Timestamp DateInvoiced;
	private String AccountNo;
	private String BPName;
	private String DocumentNo;
	private String BPAccountNo;
	
	private BigDecimal LineTotal = Env.ZERO;
	private BigDecimal TotalAmt = Env.ZERO;
	
	

	public M_RemittanceLine (ResultSet rs)	{
		try {
			MRemittanceID=rs.getInt("c_remittance_id");
			OrgName = rs.getString("orgname");
			DUNS = rs.getString("DUNS");
			AccountNo= rs.getString("accountno");
			BPAccountNo = rs.getString("partneraccount");
			GenerateDate = rs.getTimestamp("generatedate");
			ExecuteDate = rs.getTimestamp("executedate");
			DateInvoiced = rs.getTimestamp("dateinvoiced");
			BPName = rs.getString("partnername");
			DocumentNo = rs.getString("documentno");
			LineTotal = rs.getBigDecimal("grandtotal");
			TotalAmt = rs.getBigDecimal("totalamt");

		}
		catch (SQLException e)	{
			System.out.println("Error al coger datos del sql en la clase mremittanceline,e="+e);
		}	
	}
	
	public M_RemittanceLine (int M_Remittance_ID)	{
		MRemittanceID = M_Remittance_ID;
	}
	

	/**
	 * Setea el nombre de organización
	 * @param Org_Name
	 */
	
	public void setOrgName(String Org_Name){
		OrgName=Org_Name;
	}
	
	public String getOrgName(){
		return OrgName;
	}
	
	
	/**
	 * Setea el nif de la organización
	 * @param duns_id
	 */
	
	public void setDUNS(String duns_id){
		DUNS=duns_id;
	}
	
	public String getDUNS(){
		return DUNS;
	}
	
	
	/**
	 * Setea la cuenta corriente de ingreso
	 * @param account
	 */
	
	public void setAccountNo(String account){
		AccountNo=account;
	}
	
	public String getAccountNo(){
		return AccountNo;
	}
	
	
	/**
	 * Setea la cuenta corriente del cliente
	 * @param bpaccount
	 */
	
	public void setBPAccount(String bpaccount){
		BPAccountNo=bpaccount;
	}
	
	public String getBPAccount(){
		return BPAccountNo;
	}
	
	
	/**
	 * Setea la fecha de generación de la remesa
	 * @param Generate_Date
	 */
	
	public void setGenerateDate(Timestamp Generate_Date){
		GenerateDate=Generate_Date;
	}
	
	public Timestamp getGenerateDate(){
		return GenerateDate;
	}
	
	
	/**
	 * Setea la fecha de ejecución de la remesa
	 * @param Execute_Date
	 */
	
	public void setExecuteDate(Timestamp Execute_Date){
		ExecuteDate=Execute_Date;
	}
	
	public Timestamp getExecuteDate(){
		return ExecuteDate;
	}
	
	
	/**
	 * Setea la fecha de la factura
	 * @param Date_Invoiced
	 */
	
	public void setDateInvoiced(Timestamp Date_Invoiced){
		DateInvoiced=Date_Invoiced;
	}
	
	public Timestamp getDateInvoiced(){
		return DateInvoiced;
	}
	
	
	/**
	 * Setea el nombre del cliente
	 * @param bp_name
	 */
	
	public void setBPName(String bp_name){
		BPName=bp_name;
	}
	
	public String getBPName(){
		return BPName;
	}
	
	
	/**
	 * Setea el número de documento
	 * @param document_no
	 */
	
	public void setDocumentNo(String document_no){
		DocumentNo=document_no;
	}
	
	public String getDocumentNo(){
		return DocumentNo;
	}
	
	
	/**
	 * Setea el total de la linea
	 * @param total
	 */
	
	public void setLineTotal(BigDecimal total){
		LineTotal=total;
	}
	
	public BigDecimal getLineTotal(){
		return LineTotal;
	}
	
	
	/**
	 * Setea el total de la remesa
	 * @param total
	 */
	
	public void setTotalAmt(BigDecimal total){
		TotalAmt=total;
	}
	
	public BigDecimal getTotalAmt(){
		return TotalAmt;
	}
	
	
}






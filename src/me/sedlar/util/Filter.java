package me.sedlar.util;

/**
 * @author Tyler Sedlar
 * @since 10/31/14
 **/
public interface Filter<T> {

	/**
	 * Checks if the given element is acceptable.
	 * 
	 * @param t
	 *            the element to check against.
	 * @return <t>true</t> if the given element is acceptable, otherwise
	 *         <t>false</t>.
	 */
	boolean accept(T t);
}
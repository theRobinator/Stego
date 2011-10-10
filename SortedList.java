/**
 * This is a quick class that maintains a sorted doubly-linked list.
 * It's used to store the pixel positions that have already been visited.
 * @author Robert Keller, rkeller@ucsc.edu
 *
 */
public class SortedList {
	
	node first;
	int size = 0;
	
	public void insert(int val) {
		node n = new node(val);
		if (first == null) {
			first = n;
			return;
		}else if (n.value < first.value) {
			first.prev = n;
			n.next = first;
			first = n;
			return;
		}
		node curr = first;
		while (curr.next != null) {
			if (curr.next.value > n.value) {
				curr.next.prev = n;
				n.next = curr.next;
				curr.next = n;
				n.prev = curr;
				break;
			}
			curr = curr.next;
		}
		if (curr.next == null) {
			curr.next = n;
			n.prev = curr;
		}
		
		size++;
	}
	
	public boolean contains(int val) {
		node curr = first;
		while (curr != null) {
			if (curr.value == val) return true;
			if (curr.value > val) return false;
			curr = curr.next;
		}
		return false;
	}
	
	public void print() {
		node curr = first;
		while (curr != null) {
			System.out.print(curr.value+" ");
			curr = curr.next;
		}
		System.out.println();
	}
	
	static class node {
		node prev, next;
		int value;
		
		public node(int val) { value = val; }
	}
}

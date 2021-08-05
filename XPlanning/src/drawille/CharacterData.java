package drawille;

public class CharacterData implements ScreenData {
	char ch;
	
	public CharacterData(char character) {
		ch = character;
	}

	@Override
	public String toString() {
		return Character.toString(ch);
	}
}

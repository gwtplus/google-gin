package com.google.gwt.gin.higherlower.client.gin;

import com.google.gwt.gin.higherlower.client.Shuffler;
import com.google.gwt.gin.higherlower.client.bundle.DeckOfCardsClientBundle;
import com.google.gwt.gin.higherlower.client.model.Card;
import com.google.gwt.gin.higherlower.client.model.Deck;
import com.google.gwt.gin.higherlower.client.model.Rank;
import com.google.gwt.gin.higherlower.client.model.Suit;
import com.google.gwt.user.client.ui.Image;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.LinkedList;

/**
 * Provides a deck of playing cards.
 */
public class DeckProvider implements Provider<Deck> {
  private final DeckOfCardsClientBundle deckImages;
  private final Shuffler shuffler;

  @Inject // Client bundles can be injected as-is!
  public DeckProvider(DeckOfCardsClientBundle deckImages, Shuffler shuffler) {
    this.deckImages = deckImages;
    this.shuffler = shuffler;
  }

  public Deck get() {
    // TODO we could probably use a generator instead of this nonsense.
    LinkedList<Card> deck = new LinkedList<Card>();
    deck.add(new Card(Suit.CLUBS, Rank.ONE,  new Image(deckImages.ace_clubs())));
    deck.add(new Card(Suit.CLUBS, Rank.TWO,  new Image(deckImages.two_clubs())));
    deck.add(new Card(Suit.CLUBS, Rank.THREE,  new Image(deckImages.three_clubs())));
    deck.add(new Card(Suit.CLUBS, Rank.FOUR,  new Image(deckImages.four_clubs())));
    deck.add(new Card(Suit.CLUBS, Rank.FIVE,  new Image(deckImages.five_clubs())));
    deck.add(new Card(Suit.CLUBS, Rank.SIX,  new Image(deckImages.six_clubs())));
    deck.add(new Card(Suit.CLUBS, Rank.SEVEN,  new Image(deckImages.seven_clubs())));
    deck.add(new Card(Suit.CLUBS, Rank.EIGHT,  new Image(deckImages.eight_clubs())));
    deck.add(new Card(Suit.CLUBS, Rank.NINE,  new Image(deckImages.nine_clubs())));
    deck.add(new Card(Suit.CLUBS, Rank.TEN,  new Image(deckImages.ten_clubs())));
    deck.add(new Card(Suit.CLUBS, Rank.JACK,  new Image(deckImages.jack_clubs())));
    deck.add(new Card(Suit.CLUBS, Rank.QUEEN,  new Image(deckImages.queen_clubs())));
    deck.add(new Card(Suit.CLUBS, Rank.KING,  new Image(deckImages.king_clubs())));

    deck.add(new Card(Suit.DIAMONDS, Rank.ONE,  new Image(deckImages.ace_diamond())));
    deck.add(new Card(Suit.DIAMONDS, Rank.TWO,  new Image(deckImages.two_diamond())));
    deck.add(new Card(Suit.DIAMONDS, Rank.THREE,  new Image(deckImages.three_diamond())));
    deck.add(new Card(Suit.DIAMONDS, Rank.FOUR,  new Image(deckImages.four_diamond())));
    deck.add(new Card(Suit.DIAMONDS, Rank.FIVE,  new Image(deckImages.five_diamond())));
    deck.add(new Card(Suit.DIAMONDS, Rank.SIX,  new Image(deckImages.six_diamond())));
    deck.add(new Card(Suit.DIAMONDS, Rank.SEVEN,  new Image(deckImages.seven_diamond())));
    deck.add(new Card(Suit.DIAMONDS, Rank.EIGHT,  new Image(deckImages.eight_diamond())));
    deck.add(new Card(Suit.DIAMONDS, Rank.NINE,  new Image(deckImages.nine_diamond())));
    deck.add(new Card(Suit.DIAMONDS, Rank.TEN,  new Image(deckImages.ten_diamond())));
    deck.add(new Card(Suit.DIAMONDS, Rank.JACK,  new Image(deckImages.jack_diamond())));
    deck.add(new Card(Suit.DIAMONDS, Rank.QUEEN,  new Image(deckImages.queen_diamond())));
    deck.add(new Card(Suit.DIAMONDS, Rank.KING,  new Image(deckImages.king_diamond())));

    deck.add(new Card(Suit.HEARTS, Rank.ONE,  new Image(deckImages.ace_heart())));
    deck.add(new Card(Suit.HEARTS, Rank.TWO,  new Image(deckImages.two_heart())));
    deck.add(new Card(Suit.HEARTS, Rank.THREE,  new Image(deckImages.three_heart())));
    deck.add(new Card(Suit.HEARTS, Rank.FOUR,  new Image(deckImages.four_heart())));
    deck.add(new Card(Suit.HEARTS, Rank.FIVE,  new Image(deckImages.five_heart())));
    deck.add(new Card(Suit.HEARTS, Rank.SIX,  new Image(deckImages.six_heart())));
    deck.add(new Card(Suit.HEARTS, Rank.SEVEN,  new Image(deckImages.seven_heart())));
    deck.add(new Card(Suit.HEARTS, Rank.EIGHT,  new Image(deckImages.eight_heart())));
    deck.add(new Card(Suit.HEARTS, Rank.NINE,  new Image(deckImages.nine_heart())));
    deck.add(new Card(Suit.HEARTS, Rank.TEN,  new Image(deckImages.ten_heart())));
    deck.add(new Card(Suit.HEARTS, Rank.JACK,  new Image(deckImages.jack_heart())));
    deck.add(new Card(Suit.HEARTS, Rank.QUEEN,  new Image(deckImages.queen_heart())));
    deck.add(new Card(Suit.HEARTS, Rank.KING,  new Image(deckImages.king_heart())));

    deck.add(new Card(Suit.SPADES, Rank.ONE,  new Image(deckImages.ace_spade())));
    deck.add(new Card(Suit.SPADES, Rank.TWO,  new Image(deckImages.two_spade())));
    deck.add(new Card(Suit.SPADES, Rank.THREE,  new Image(deckImages.three_spade())));
    deck.add(new Card(Suit.SPADES, Rank.FOUR,  new Image(deckImages.four_spade())));
    deck.add(new Card(Suit.SPADES, Rank.FIVE,  new Image(deckImages.five_spade())));
    deck.add(new Card(Suit.SPADES, Rank.SIX,  new Image(deckImages.six_spade())));
    deck.add(new Card(Suit.SPADES, Rank.SEVEN,  new Image(deckImages.seven_spade())));
    deck.add(new Card(Suit.SPADES, Rank.EIGHT,  new Image(deckImages.eight_spade())));
    deck.add(new Card(Suit.SPADES, Rank.NINE,  new Image(deckImages.nine_spade())));
    deck.add(new Card(Suit.SPADES, Rank.TEN,  new Image(deckImages.ten_spade())));
    deck.add(new Card(Suit.SPADES, Rank.JACK,  new Image(deckImages.jack_spade())));
    deck.add(new Card(Suit.SPADES, Rank.QUEEN,  new Image(deckImages.queen_spade())));
    deck.add(new Card(Suit.SPADES, Rank.KING,  new Image(deckImages.king_spade())));

    return new Deck(deck, shuffler);
  }
}

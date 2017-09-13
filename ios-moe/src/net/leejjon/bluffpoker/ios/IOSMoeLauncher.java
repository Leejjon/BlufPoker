package net.leejjon.bluffpoker.ios;

import apple.contacts.CNContact;
import apple.contacts.CNContactFetchRequest;
import apple.contacts.CNContactStore;
import apple.contacts.c.Contacts;
import apple.foundation.NSArray;
import apple.foundation.NSError;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.iosmoe.IOSApplicationConfiguration;
import net.leejjon.bluffpoker.BluffPokerGame;
import net.leejjon.bluffpoker.interfaces.ContactsRequesterInterface;
import net.leejjon.bluffpoker.listener.ModifyPlayerListener;
import org.moe.natj.general.Pointer;

import apple.uikit.c.UIKit;
import org.moe.natj.general.ptr.BoolPtr;
import org.moe.natj.general.ptr.Ptr;
import org.moe.natj.general.ptr.impl.PtrFactory;
import org.moe.natj.objc.ObjCException;

import java.util.Set;
import java.util.TreeSet;

public class IOSMoeLauncher extends BluffPokerIOSApplication.Delegate implements ContactsRequesterInterface {

    protected IOSMoeLauncher(Pointer peer) {
        super(peer);
    }

    private BluffPokerGame bluffPokerGame;

    private BluffPokerIOSApplication bluffPokerIOSApplication;

    @Override
    protected BluffPokerIOSApplication createApplication() {
        IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        config.useAccelerometer = true;

        bluffPokerGame = new BluffPokerGame(this, 2);
//        MyGdxGame bluffPokerGame = new MyGdxGame();

        bluffPokerIOSApplication = new BluffPokerIOSApplication(bluffPokerGame, config);
        return bluffPokerIOSApplication;
    }

    public static void main(String[] argv) {
        UIKit.UIApplicationMain(0, null, null, IOSMoeLauncher.class.getName());
    }

    @Override
    public String getDeviceOwnerName() {
//        func unifiedMeContactWithKeys(toFetch: [CNKeyDescriptor])
//
//        Fetches the unified contact that is the "me" card.

        return "Player 1";
    }

    /**
     * Code ripped from: https://gist.github.com/willthink/024f1394474e70904728
     * <p>
     * Contact permissions needed to be added to the Info.plist for this code to work.
     * https://github.com/yonahforst/react-native-permissions/issues/38
     *
     * @param listener
     * @param alreadyExistingPlayers
     */
    @Override
    public void initiateSelectContacts(ModifyPlayerListener listener, Set<String> alreadyExistingPlayers) {
        CNContactStore contactStore = bluffPokerIOSApplication.getContactStore();

        contactStore.requestAccessForEntityTypeCompletionHandler(apple.contacts.enums.CNEntityType.CNEntityTypeContacts, new CNContactStore.Block_requestAccessForEntityTypeCompletionHandler() {
            @Override
            public void call_requestAccessForEntityTypeCompletionHandler(boolean success, NSError error) {
                if (success) {
                    Gdx.app.log("bluffpoker", "The user gave us access to the contacts in the phonebook.");
                    selectContacts(listener, alreadyExistingPlayers, contactStore);
                } else {
                    Gdx.app.log("bluffpoker", "Did not have access to contacts. Error code: " + error.code());
                }
            }
        });
    }

    private void selectContacts(ModifyPlayerListener listener, Set<String> alreadyExistingPlayers, CNContactStore contactStore) {
        Ptr<NSError> error = PtrFactory.newObjectReference(NSError.class);
        try {
            NSArray<?> keysToFetch = NSArray.arrayWithObjects(Contacts.CNContactGivenNameKey(), Contacts.CNContactFamilyNameKey());
            CNContactFetchRequest request = CNContactFetchRequest.alloc().initWithKeysToFetch(keysToFetch);
            Set<String> players = new TreeSet<>();

            // Seems to retrieve all contacts synchronous before continuing. Weird since anonymous classes are mainly used for asynchronous calls.
            contactStore.enumerateContactsWithFetchRequestErrorUsingBlock(request, error, new CNContactStore.Block_enumerateContactsWithFetchRequestErrorUsingBlock() {
                @Override
                public void call_enumerateContactsWithFetchRequestErrorUsingBlock(CNContact contact, BoolPtr arg1) {
                    String name = contact.givenName() + contact.familyName();
                    if (!(name.length() == 0) && !players.contains(name) && !alreadyExistingPlayers.contains(name)) {
                        players.add(name);
                    }
                }
            });
            listener.selectFromPhoneBook(players.toArray(new String[players.size()]));
        } catch (ObjCException e) {
            e.printStackTrace();
            Gdx.app.log(BluffPokerGame.TAG, e.getMessage());
            if (error != null && error.get() != null) {
                Gdx.app.log(BluffPokerGame.TAG, "Error code: " + error.get().code());
            }
        }
    }
}
